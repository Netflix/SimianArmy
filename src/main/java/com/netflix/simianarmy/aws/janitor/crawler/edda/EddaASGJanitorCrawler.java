/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.netflix.simianarmy.aws.janitor.crawler.edda;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import com.netflix.simianarmy.client.edda.EddaClient;
import com.netflix.simianarmy.janitor.JanitorCrawler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The crawler to crawl AWS auto scaling groups for janitor monkey using Edda.
 */
public class EddaASGJanitorCrawler implements JanitorCrawler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EddaASGJanitorCrawler.class);

    /** The name representing the additional field name of instance ids. */
    public static final String ASG_FIELD_INSTANCES = "INSTANCES";

    /** The name representing the additional field name of max ASG size. */
    public static final String ASG_FIELD_MAX_SIZE = "MAX_SIZE";

    /** The name representing the additional field name of ELB names. */
    public static final String ASG_FIELD_ELBS = "ELBS";

    /** The name representing the additional field name of launch configuration name. */
    public static final String ASG_FIELD_LC_NAME = "LAUNCH_CONFIGURATION_NAME";

    /** The name representing the additional field name of launch configuration creation time. */
    public static final String ASG_FIELD_LC_CREATION_TIME = "LAUNCH_CONFIGURATION_CREATION_TIME";

    /** The name representing the additional field name of ASG suspension time from ELB. */
    public static final String ASG_FIELD_SUSPENSION_TIME = "ASG_SUSPENSION_TIME";

    /** The name representing the additional field name of ASG's last change/activity time. */
    public static final String ASG_FIELD_LAST_CHANGE_TIME = "ASG_LAST_CHANGE_TIME";

    /** The regular expression patter below is for the termination reason added by AWS when
     * an ASG is suspended from ELB's traffic.
     */
    private static final Pattern SUSPENSION_REASON_PATTERN =
            Pattern.compile("User suspended at (\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}).*");

    private final EddaClient eddaClient;
    private final List<String> regions = Lists.newArrayList();
    private final Map<String, Map<String, Long>> regionToAsgToLastChangeTime = Maps.newHashMap();

    /**
     * Instantiates a new basic ASG crawler.
     * @param eddaClient
     *            the Edda client
     * @param regions
     *            the regions the crawler will crawl resources for
     */
    public EddaASGJanitorCrawler(EddaClient eddaClient, String... regions) {
        Validate.notNull(eddaClient);
        this.eddaClient = eddaClient;
        Validate.notNull(regions);
        for (String region : regions) {
            this.regions.add(region);
        }
    }

    @Override
    public EnumSet<? extends ResourceType> resourceTypes() {
        return EnumSet.of(AWSResourceType.ASG);
    }

    @Override
    public List<Resource> resources(ResourceType resourceType) {
        if ("ASG".equals(resourceType.name())) {
            return getASGResources();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Resource> resources(String... asgNames) {
        return getASGResources(asgNames);
    }

    @Override
    public String getOwnerEmailForResource(Resource resource) {
        Validate.notNull(resource);
        return resource.getTag(BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY);
    }

    private List<Resource> getASGResources(String... asgNames) {
        refreshAsgLastChangeTime();
        List<Resource> resources = Lists.newArrayList();
        for (String region : regions) {
            resources.addAll(getASGResourcesInRegion(region, asgNames));
        }
        return resources;
    }

    private List<Resource> getASGResourcesInRegion(String region, String... asgNames) {
        String url = eddaClient.getBaseUrl(region) + "/aws/autoScalingGroups;";
        if (asgNames != null && asgNames.length != 0) {
            url += StringUtils.join(asgNames, ',');
            LOGGER.info(String.format("Getting ASGs in region %s for %d ids", region, asgNames.length));
        } else {
            LOGGER.info(String.format("Getting all ASGs in region %s", region));
        }
        url += ";_expand:(autoScalingGroupName,createdTime,maxSize,suspendedProcesses:(processName,suspensionReason),"
               + "tags:(key,value),instances:(instanceId),loadBalancerNames,launchConfigurationName)";

        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get Jason node from edda for ASGs in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }

        Map<String, Long> lcNameToCreationTime = getLaunchConfigCreationTimes(region);
        List<Resource> resources = Lists.newArrayList();
        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            resources.add(parseJsonElementToresource(region, it.next(), lcNameToCreationTime));
        }
        return resources;
    }

    private Resource parseJsonElementToresource(String region, JsonNode jsonNode
            , Map<String, Long> lcNameToCreationTime) {
        Validate.notNull(jsonNode);

        String asgName = jsonNode.get("autoScalingGroupName").getTextValue();
        long createdTime = jsonNode.get("createdTime").getLongValue();

        Resource resource = new AWSResource().withId(asgName).withRegion(region)
                .withResourceType(AWSResourceType.ASG)
                .withLaunchTime(new Date(createdTime));

        JsonNode tags = jsonNode.get("tags");
        if (tags == null || !tags.isArray() || tags.size() == 0) {
            LOGGER.debug(String.format("No tags is found for %s", resource.getId()));
        } else {
            for (Iterator<JsonNode> it = tags.getElements(); it.hasNext();) {
                JsonNode tag = it.next();
                String key = tag.get("key").getTextValue();
                String value = tag.get("value").getTextValue();
                resource.setTag(key, value);
            }
        }

        String owner = getOwnerEmailForResource(resource);
        if (owner != null) {
            resource.setOwnerEmail(owner);
        }
        JsonNode maxSize = jsonNode.get("maxSize");
        if (maxSize != null) {
            resource.setAdditionalField(ASG_FIELD_MAX_SIZE, String.valueOf(maxSize.getIntValue()));
        }
        // Adds instances and ELBs as additional fields.
        JsonNode instances = jsonNode.get("instances");
        resource.setDescription(String.format("%d instances", instances.size()));
        List<String> instanceIds = Lists.newArrayList();
        for (Iterator<JsonNode> it = instances.getElements(); it.hasNext();) {
            instanceIds.add(it.next().get("instanceId").getTextValue());
        }
        resource.setAdditionalField(ASG_FIELD_INSTANCES, StringUtils.join(instanceIds, ","));
        JsonNode elbs = jsonNode.get("loadBalancerNames");
        List<String> elbNames = Lists.newArrayList();
        for (Iterator<JsonNode> it = elbs.getElements(); it.hasNext();) {
            elbNames.add(it.next().getTextValue());
        }
        resource.setAdditionalField(ASG_FIELD_ELBS, StringUtils.join(elbNames, ","));

        JsonNode lc = jsonNode.get("launchConfigurationName");
        if (lc != null) {
            String lcName = lc.getTextValue();
            Long lcCreationTime = lcNameToCreationTime.get(lcName);
            if (lcName != null) {
                resource.setAdditionalField(ASG_FIELD_LC_NAME, lcName);
            }
            if (lcCreationTime != null) {
                resource.setAdditionalField(ASG_FIELD_LC_CREATION_TIME, String.valueOf(lcCreationTime));
            }
        }
        // sets the field for the time when the ASG's traffic is suspended from ELB
        JsonNode suspendedProcesses = jsonNode.get("suspendedProcesses");
        for (Iterator<JsonNode> it = suspendedProcesses.getElements(); it.hasNext();) {
            JsonNode sp = it.next();
            if ("AddToLoadBalancer".equals(sp.get("processName").getTextValue())) {
                String suspensionTime = getSuspensionTimeString(sp.get("suspensionReason").getTextValue());
                if (suspensionTime != null) {
                    LOGGER.info(String.format("Suspension time of ASG %s is %s",
                            asgName, suspensionTime));
                    resource.setAdditionalField(ASG_FIELD_SUSPENSION_TIME, suspensionTime);
                    break;
                }
            }
        }
        Long lastChangeTime = regionToAsgToLastChangeTime.get(region).get(asgName);
        if (lastChangeTime != null) {
            resource.setAdditionalField(ASG_FIELD_LAST_CHANGE_TIME, String.valueOf(lastChangeTime));
        }
        return resource;

    }

    private Map<String, Long> getLaunchConfigCreationTimes(String region) {
        LOGGER.info(String.format("Getting launch configuration creation times in region %s", region));

        String url = eddaClient.getBaseUrl(region)
                + "/aws/launchConfigurations;_expand:(launchConfigurationName,createdTime)";
        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get Jason node from edda for lc creation times in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }

        Map<String, Long> nameToCreationTime = Maps.newHashMap();
        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            JsonNode elem = it.next();
            nameToCreationTime.put(elem.get("launchConfigurationName").getTextValue(),
                    elem.get("createdTime").getLongValue());
        }
        return nameToCreationTime;
    }

    private String getSuspensionTimeString(String suspensionReason) {
        if (suspensionReason == null) {
            return null;
        }
        Matcher matcher = SUSPENSION_REASON_PATTERN.matcher(suspensionReason);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    private void refreshAsgLastChangeTime() {
        regionToAsgToLastChangeTime.clear();
        for (String region : regions) {
            LOGGER.info(String.format("Getting ASG last change time in region %s", region));
            Map<String, Long> asgToLastChangeTime = regionToAsgToLastChangeTime.get(region);
            if (asgToLastChangeTime == null) {
                asgToLastChangeTime = Maps.newHashMap();
                regionToAsgToLastChangeTime.put(region, asgToLastChangeTime);
            }
            String url = eddaClient.getBaseUrl(region) + "/aws/autoScalingGroups;"
                    + ";_expand;_meta:(stime,data:(autoScalingGroupName))";

            JsonNode jsonNode = null;
            try {
                jsonNode = eddaClient.getJsonNodeFromUrl(url);
            } catch (Exception e) {
                LOGGER.error(String.format(
                        "Failed to get Jason node from edda for ASG last change time in region %s.", region), e);
            }

            if (jsonNode == null || !jsonNode.isArray()) {
                throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s",
                        url, jsonNode));
            }

            for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
                JsonNode asg = it.next();
                String asgName = asg.get("data").get("autoScalingGroupName").getTextValue();
                Long lastChangeTime = asg.get("stime").asLong();
                LOGGER.debug(String.format("The last change time of ASG %s is %s", asgName,
                        new DateTime(lastChangeTime)));
                asgToLastChangeTime.put(asgName, lastChangeTime);
            }
        }
    }

}

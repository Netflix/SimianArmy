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
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.aws.janitor.crawler.InstanceJanitorCrawler;
import com.netflix.simianarmy.client.edda.EddaClient;
import com.netflix.simianarmy.janitor.JanitorCrawler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The crawler to crawl AWS instances for janitor monkey using Edda.
 */
public class EddaInstanceJanitorCrawler implements JanitorCrawler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EddaInstanceJanitorCrawler.class);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    private final EddaClient eddaClient;
    private final List<String> regions = Lists.newArrayList();
    private final Map<String, String> instanceToAsg = Maps.newHashMap();


    /**
     * Instantiates a new basic instance crawler.
     * @param eddaClient
     *            the Edda client
     * @param regions
     *            the regions the crawler will crawl resources for
     */
    public EddaInstanceJanitorCrawler(EddaClient eddaClient, String... regions) {
        Validate.notNull(eddaClient);
        this.eddaClient = eddaClient;
        Validate.notNull(regions);
        for (String region : regions) {
            this.regions.add(region);
        }
    }

    @Override
    public EnumSet<?> resourceTypes() {
        return EnumSet.of(AWSResourceType.INSTANCE);
    }

    @Override
    public List<Resource> resources(Enum resourceType) {
        if ("INSTANCE".equals(resourceType.name())) {
            return getInstanceResources();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Resource> resources(String... resourceIds) {
        return getInstanceResources(resourceIds);
    }

    @Override
    public String getOwnerEmailForResource(Resource resource) {
        return null;
    }

    private List<Resource> getInstanceResources(String... instanceIds) {
        List<Resource> resources = Lists.newArrayList();
        for (String region : regions) {
            resources.addAll(getInstanceResourcesInRegion(region, instanceIds));
        }
        return resources;
    }

    private List<Resource> getInstanceResourcesInRegion(String region, String... instanceIds) {
        refreshAsgInstances();

        String url = eddaClient.getBaseUrl(region) + "/view/instances;";
        if (instanceIds != null && instanceIds.length != 0) {
            url += StringUtils.join(instanceIds, ',');
            LOGGER.info(String.format("Getting instances in region %s for %d ids", region, instanceIds.length));
        } else {
            LOGGER.info(String.format("Getting all instances in region %s", region));
        }
        url += ";state.name=running;_expand:(instanceId,launchTime,state:(name),instanceType"
                + ",publicDnsName,tags:(key,value))";

        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get Jason node from edda for instances in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }

        List<Resource> resources = Lists.newArrayList();
        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            resources.add(parseJsonElementToInstanceResource(region, it.next()));
        }
        return resources;
    }

    private Resource parseJsonElementToInstanceResource(String region, JsonNode jsonNode) {
        Validate.notNull(jsonNode);

        String instanceId = jsonNode.get("instanceId").getTextValue();
        long launchTime = jsonNode.get("launchTime").getLongValue();

        Resource resource = new AWSResource().withId(instanceId).withRegion(region)
                .withResourceType(AWSResourceType.INSTANCE)
                .withLaunchTime(new Date(launchTime));

        JsonNode publicDnsName = jsonNode.get("publicDnsName");
        String description = String.format("type=%s; host=%s",
                jsonNode.get("instanceType").getTextValue(),
                publicDnsName == null ? "" : publicDnsName.getTextValue());
        resource.setDescription(description);

        String owner = getOwnerEmailForResource(resource);
        resource.setOwnerEmail(owner);
        JsonNode tags = jsonNode.get("tags");
        String asgName = null;
        if (tags == null || !tags.isArray() || tags.size() == 0) {
            LOGGER.debug(String.format("No tags is found for %s", resource.getId()));
        } else {
            for (Iterator<JsonNode> it = tags.getElements(); it.hasNext();) {
                JsonNode tag = it.next();
                String key = tag.get("key").getTextValue();
                String value = tag.get("value").getTextValue();
                resource.setTag(key, value);
                if ("aws:autoscaling:groupName".equals(key)) {
                    asgName = value;
                } else if (owner == null && "owner".equals(key)) {
                    resource.setOwnerEmail(value);
                }
            }
            resource.setDescription(description.toString());
        }
        // If we cannot find ASG name in tags, use the map for the ASG name
        if (asgName == null) {
            asgName = instanceToAsg.get(instanceId);
            if (asgName != null) {
                LOGGER.debug(String.format("Failed to find ASG name in tags of %s, use the ASG name %s from map",
                        instanceId, asgName));
            }
        }
        if (asgName != null) {
            resource.setAdditionalField(InstanceJanitorCrawler.INSTANCE_FIELD_ASG_NAME, asgName);
        }
        ((AWSResource) resource).setAWSResourceState(jsonNode.get("state").get("name").getTextValue());
        return resource;

    }

    private void refreshAsgInstances() {
        instanceToAsg.clear();
        for (String region : regions) {
            LOGGER.info(String.format("Getting ASG instances in region %s", region));
            String url = eddaClient.getBaseUrl(region) + "/aws/autoScalingGroups"
                    + ";_expand:(autoScalingGroupName,instances:(instanceId))";

            JsonNode jsonNode = null;
            try {
                jsonNode = eddaClient.getJsonNodeFromUrl(url);
            } catch (Exception e) {
                LOGGER.error(String.format(
                        "Failed to get Jason node from edda for ASGs in region %s.", region), e);
            }

            if (jsonNode == null || !jsonNode.isArray()) {
                throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s",
                        url, jsonNode));
            }

            for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
                JsonNode asg = it.next();
                String asgName = asg.get("autoScalingGroupName").getTextValue();
                JsonNode instances = asg.get("instances");
                if (instances == null || instances.isNull() || !instances.isArray() || instances.size() == 0) {
                    continue;
                }
                for (Iterator<JsonNode> instanceIt = instances.getElements(); instanceIt.hasNext();) {
                    JsonNode instance = instanceIt.next();
                    instanceToAsg.put(instance.get("instanceId").getTextValue(), asgName);
                }
            }

        }
    }
}

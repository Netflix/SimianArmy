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
import com.google.common.collect.Sets;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.edda.EddaClient;
import com.netflix.simianarmy.janitor.JanitorCrawler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The crawler to crawl AWS launch configurations for janitor monkey using Edda.
 */
public class EddaLaunchConfigJanitorCrawler implements JanitorCrawler {

    /** The name representing the additional field name of a flag indicating if the launch config
     * if used by an auto scaling group. */
    public static final String LAUNCH_CONFIG_FIELD_USED_BY_ASG = "USED_BY_ASG";

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EddaLaunchConfigJanitorCrawler.class);

    private final EddaClient eddaClient;
    private final List<String> regions = Lists.newArrayList();


    /**
     * Instantiates a new basic launch configuration crawler.
     * @param eddaClient
     *            the Edda client
     * @param regions
     *            the regions the crawler will crawl resources for
     */
    public EddaLaunchConfigJanitorCrawler(EddaClient eddaClient, String... regions) {
        Validate.notNull(eddaClient);
        this.eddaClient = eddaClient;
        Validate.notNull(regions);
        for (String region : regions) {
            this.regions.add(region);
        }
    }

    @Override
    public EnumSet<? extends ResourceType> resourceTypes() {
        return EnumSet.of(AWSResourceType.LAUNCH_CONFIG);
    }

    @Override
    public List<Resource> resources(ResourceType resourceType) {
        if ("LAUNCH_CONFIG".equals(resourceType.name())) {
            return getLaunchConfigResources();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Resource> resources(String... resourceIds) {
        return getLaunchConfigResources(resourceIds);
    }

    private List<Resource> getLaunchConfigResources(String... launchConfigNames) {
        List<Resource> resources = Lists.newArrayList();
        for (String region : regions) {
            resources.addAll(getLaunchConfigResourcesInRegion(region, launchConfigNames));
        }
        return resources;
    }

    @Override
    public String getOwnerEmailForResource(Resource resource) {
        //Launch Configs don't have Tags
        return null;
    }

    private List<Resource> getLaunchConfigResourcesInRegion(String region, String... launchConfigNames) {
        String url = eddaClient.getBaseUrl(region) + "/aws/launchConfigurations;";
        if (launchConfigNames != null && launchConfigNames.length != 0) {
            url += StringUtils.join(launchConfigNames, ',');
            LOGGER.info(String.format("Getting launch configurations in region %s for %d ids",
                    region, launchConfigNames.length));
        } else {
            LOGGER.info(String.format("Getting all launch configurations in region %s", region));
        }
        url += ";_expand:(launchConfigurationName,createdTime)";

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

        Set<String> usedLCs = getLaunchConfigsInUse(region);

        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            JsonNode launchConfiguration = it.next();
            String lcName = launchConfiguration.get("launchConfigurationName").getTextValue();
            Resource lcResource = new AWSResource().withId(lcName)
                    .withRegion(region).withResourceType(AWSResourceType.LAUNCH_CONFIG)
                    .withLaunchTime(new Date(launchConfiguration.get("createdTime").getLongValue()));
            lcResource.setOwnerEmail(getOwnerEmailForResource(lcResource));

            lcResource.setAdditionalField(LAUNCH_CONFIG_FIELD_USED_BY_ASG, String.valueOf(usedLCs.contains(lcName)));
            resources.add(lcResource);
        }
        return resources;
    }

    /**
     * Gets the launch configs that are currently in use by at least one ASG in a region.
     * @param region the region
     * @return the set of launch config names
     */
    private Set<String> getLaunchConfigsInUse(String region) {
        LOGGER.info(String.format("Getting all launch configurations in use in region %s", region));
        String url = eddaClient.getBaseUrl(region) + "/aws/autoScalingGroups;_expand:(launchConfigurationName)";

        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get Jason node from edda for launch configs in use in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }

        Set<String> launchConfigs = Sets.newHashSet();
        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            launchConfigs.add(it.next().get("launchConfigurationName").getTextValue());
        }
        return launchConfigs;
    }
}

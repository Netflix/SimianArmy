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

package com.netflix.simianarmy.aws.janitor.crawler;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The crawler to crawl AWS launch configurations for janitor monkey.
 */
public class LaunchConfigJanitorCrawler extends AbstractAWSJanitorCrawler {

    /** The name representing the additional field name of a flag indicating if the launch config
     * if used by an auto scaling group. */
    public static final String LAUNCH_CONFIG_FIELD_USED_BY_ASG = "USED_BY_ASG";

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchConfigJanitorCrawler.class);

    /**
     * Instantiates a new basic launch configuration crawler.
     * @param awsClient
     *            the aws client
     */
    public LaunchConfigJanitorCrawler(AWSClient awsClient) {
        super(awsClient);
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

        AWSClient awsClient = getAWSClient();

        Set<String> usedLCs = Sets.newHashSet();
        for (AutoScalingGroup asg : awsClient.describeAutoScalingGroups()) {
            usedLCs.add(asg.getLaunchConfigurationName());
        }

        for (LaunchConfiguration launchConfiguration : awsClient.describeLaunchConfigurations(launchConfigNames)) {
            String lcName = launchConfiguration.getLaunchConfigurationName();
            Resource lcResource = new AWSResource().withId(lcName)
                    .withRegion(getAWSClient().region()).withResourceType(AWSResourceType.LAUNCH_CONFIG)
                    .withLaunchTime(launchConfiguration.getCreatedTime());
            lcResource.setOwnerEmail(getOwnerEmailForResource(lcResource));

            lcResource.setAdditionalField(LAUNCH_CONFIG_FIELD_USED_BY_ASG, String.valueOf(usedLCs.contains(lcName)));
            resources.add(lcResource);
        }
        return resources;
    }
}

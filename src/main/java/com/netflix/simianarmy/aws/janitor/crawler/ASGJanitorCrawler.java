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

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.SuspendedProcess;
import com.amazonaws.services.autoscaling.model.TagDescription;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;

/**
 * The crawler to crawl AWS auto scaling groups for janitor monkey.
 */
public class ASGJanitorCrawler extends AbstractAWSJanitorCrawler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ASGJanitorCrawler.class);

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

    private final Map<String, LaunchConfiguration> nameToLaunchConfig = new HashMap<String, LaunchConfiguration>();

    /** The regular expression patter below is for the termination reason added by AWS when
     * an ASG is suspended from ELB's traffic.
     */
    private static final Pattern SUSPENSION_REASON_PATTERN =
            Pattern.compile("User suspended at (\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}).*");

    /** The date format used to print or parse the suspension time value. **/
    public static final DateTimeFormatter SUSPENSION_TIME_FORMATTER =
            DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * Instantiates a new basic ASG crawler.
     * @param awsClient
     *            the aws client
     */
    public ASGJanitorCrawler(AWSClient awsClient) {
        super(awsClient);
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

    private List<Resource> getASGResources(String... asgNames) {
        AWSClient awsClient = getAWSClient();

        List<LaunchConfiguration> launchConfigurations = awsClient.describeLaunchConfigurations();
        for (LaunchConfiguration lc : launchConfigurations) {
            nameToLaunchConfig.put(lc.getLaunchConfigurationName(), lc);
        }

        List<Resource> resources = new LinkedList<Resource>();
        for (AutoScalingGroup asg : awsClient.describeAutoScalingGroups(asgNames)) {
            Resource asgResource = new AWSResource().withId(asg.getAutoScalingGroupName())
                    .withResourceType(AWSResourceType.ASG).withRegion(awsClient.region())
                    .withLaunchTime(asg.getCreatedTime());
            for (TagDescription tag : asg.getTags()) {
                asgResource.setTag(tag.getKey(), tag.getValue());
            }
            asgResource.setDescription(String.format("%d instances", asg.getInstances().size()));
            asgResource.setOwnerEmail(getOwnerEmailForResource(asgResource));
            if (asg.getStatus() != null) {
                ((AWSResource) asgResource).setAWSResourceState(asg.getStatus());
            }
            Integer maxSize = asg.getMaxSize();
            if (maxSize != null) {
                asgResource.setAdditionalField(ASG_FIELD_MAX_SIZE, String.valueOf(maxSize));
            }
            // Adds instances and ELBs as additional fields.
            List<String> instances = new ArrayList<String>();
            for (Instance instance : asg.getInstances()) {
                instances.add(instance.getInstanceId());
            }
            asgResource.setAdditionalField(ASG_FIELD_INSTANCES, StringUtils.join(instances, ","));
            asgResource.setAdditionalField(ASG_FIELD_ELBS,
                    StringUtils.join(asg.getLoadBalancerNames(), ","));
            String lcName = asg.getLaunchConfigurationName();
            LaunchConfiguration lc = nameToLaunchConfig.get(lcName);
            if (lc != null) {
                asgResource.setAdditionalField(ASG_FIELD_LC_NAME, lcName);
            }
            if (lc != null && lc.getCreatedTime() != null) {
                asgResource.setAdditionalField(ASG_FIELD_LC_CREATION_TIME,
                        String.valueOf(lc.getCreatedTime().getTime()));
            }
            // sets the field for the time when the ASG's traffic is suspended from ELB
            for (SuspendedProcess sp : asg.getSuspendedProcesses()) {
                if ("AddToLoadBalancer".equals(sp.getProcessName())) {
                    String suspensionTime = getSuspensionTimeString(sp.getSuspensionReason());
                    if (suspensionTime != null) {
                        LOGGER.info(String.format("Suspension time of ASG %s is %s",
                                asg.getAutoScalingGroupName(), suspensionTime));
                        asgResource.setAdditionalField(ASG_FIELD_SUSPENSION_TIME, suspensionTime);
                        break;
                    }
                }
            }
            resources.add(asgResource);
        }
        return resources;
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
}

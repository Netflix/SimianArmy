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

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;

/**
 * The crawler to crawl AWS instances for janitor monkey.
 */
public class InstanceJanitorCrawler extends AbstractAWSJanitorCrawler {

    /** The name representing the additional field name of ASG's name. */
    public static final String INSTANCE_FIELD_ASG_NAME = "ASG_NAME";

    /** The name representing the additional field name of the OpsWork stack name. */
    public static final String  INSTANCE_FIELD_OPSWORKS_STACK_NAME = "OPSWORKS_STACK_NAME";

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceJanitorCrawler.class);

    /**
     * Instantiates a new basic instance crawler.
     * @param awsClient
     *            the aws client
     */
    public InstanceJanitorCrawler(AWSClient awsClient) {
        super(awsClient);
    }

    @Override
    public EnumSet<? extends ResourceType> resourceTypes() {
        return EnumSet.of(AWSResourceType.INSTANCE);
    }

    @Override
    public List<Resource> resources(ResourceType resourceType) {
        if ("INSTANCE".equals(resourceType.name())) {
            return getInstanceResources();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Resource> resources(String... resourceIds) {
        return getInstanceResources(resourceIds);
    }

    private List<Resource> getInstanceResources(String... instanceIds) {
        List<Resource> resources = new LinkedList<Resource>();

        AWSClient awsClient = getAWSClient();
        Map<String, AutoScalingInstanceDetails> idToASGInstance = new HashMap<String, AutoScalingInstanceDetails>();
        for (AutoScalingInstanceDetails instanceDetails : awsClient.describeAutoScalingInstances(instanceIds)) {
            idToASGInstance.put(instanceDetails.getInstanceId(), instanceDetails);
        }

        for (Instance instance : awsClient.describeInstances(instanceIds)) {
            Resource instanceResource = new AWSResource().withId(instance.getInstanceId())
                    .withRegion(getAWSClient().region()).withResourceType(AWSResourceType.INSTANCE)
                    .withLaunchTime(instance.getLaunchTime());
            for (Tag tag : instance.getTags()) {
                instanceResource.setTag(tag.getKey(), tag.getValue());
            }
            String description = String.format("type=%s; host=%s", instance.getInstanceType(),
                    instance.getPublicDnsName() == null ? "" : instance.getPublicDnsName());
            instanceResource.setDescription(description);
            instanceResource.setOwnerEmail(getOwnerEmailForResource(instanceResource));

            String asgName = getAsgName(instanceResource, idToASGInstance);
            if (asgName != null) {
                instanceResource.setAdditionalField(INSTANCE_FIELD_ASG_NAME, asgName);
                LOGGER.info(String.format("instance %s has a ASG tag name %s.", instanceResource.getId(), asgName));
            }
            String opsworksStackName = getOpsWorksStackName(instanceResource);
            if (opsworksStackName != null) {
                instanceResource.setAdditionalField(INSTANCE_FIELD_OPSWORKS_STACK_NAME, opsworksStackName);
                LOGGER.info(String.format("instance %s is part of an OpsWorks stack named %s.", instanceResource.getId(), opsworksStackName));
            }
            if (instance.getState() != null) {
                ((AWSResource) instanceResource).setAWSResourceState(instance.getState().getName());
            }
            resources.add(instanceResource);
        }
        return resources;
    }

    private String getAsgName(Resource instanceResource, Map<String, AutoScalingInstanceDetails> idToASGInstance) {
        String asgName = instanceResource.getTag("aws:autoscaling:groupName");
        if (asgName == null) {
            // At most times the aws:autoscaling:groupName tag has the ASG name, but there are cases
            // that the instance is not correctly tagged and we can find the ASG name from AutoScaling
            // service.
            AutoScalingInstanceDetails instanceDetails = idToASGInstance.get(instanceResource.getId());
            if (instanceDetails != null) {
                asgName = instanceDetails.getAutoScalingGroupName();
            }
        }
        return asgName;
    }

    private String getOpsWorksStackName(Resource instanceResource) {
        return instanceResource.getTag("opsworks:stack");
    }
}

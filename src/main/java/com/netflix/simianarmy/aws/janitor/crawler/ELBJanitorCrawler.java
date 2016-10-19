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
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.Tag;
import com.amazonaws.services.elasticloadbalancing.model.TagDescription;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The crawler to crawl AWS instances for janitor monkey.
 */
public class ELBJanitorCrawler extends AbstractAWSJanitorCrawler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ELBJanitorCrawler.class);

    /**
     * Instantiates a new basic instance crawler.
     * @param awsClient
     *            the aws client
     */
    public ELBJanitorCrawler(AWSClient awsClient) {
        super(awsClient);
    }

    @Override
    public EnumSet<? extends ResourceType> resourceTypes() {
        return EnumSet.of(AWSResourceType.ELB);
    }

    @Override
    public List<Resource> resources(ResourceType resourceType) {
        if ("ELB".equals(resourceType.name())) {
            return getELBResources();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Resource> resources(String... resourceIds) {
        return getELBResources(resourceIds);
    }

    private List<Resource> getELBResources(String... elbNames) {
        List<Resource> resources = new LinkedList<Resource>();
        AWSClient awsClient = getAWSClient();

        for (LoadBalancerDescription elb : awsClient.describeElasticLoadBalancers(elbNames)) {
            Resource resource = new AWSResource().withId(elb.getLoadBalancerName())
                    .withRegion(getAWSClient().region()).withResourceType(AWSResourceType.ELB)
                    .withLaunchTime(elb.getCreatedTime());
            resource.setOwnerEmail(getOwnerEmailForResource(resource));
            resources.add(resource);
            List<Instance> instances = elb.getInstances();
            if (instances == null || instances.size() == 0) {
                resource.setAdditionalField("instanceCount", "0");
                resource.setDescription("instances=none");
                LOGGER.debug(String.format("No instances found for ELB %s", resource.getId()));
            } else {
                resource.setAdditionalField("instanceCount", "" + instances.size());
                ArrayList<String> instanceList = new ArrayList<String>(instances.size());
                LOGGER.debug(String.format("Found %d instances for ELB %s", instances.size(), resource.getId()));
                for (Instance instance : instances) {
                    String instanceId = instance.getInstanceId();
                    instanceList.add(instanceId);
                }
                String instancesStr = StringUtils.join(instanceList,",");
                resource.setDescription(String.format("instances=%s", instances));
                LOGGER.debug(String.format("Resource ELB %s has instances %s", resource.getId(), instancesStr));
            }

            for(TagDescription tagDescription : awsClient.describeElasticLoadBalancerTags(resource.getId())) {
                for(Tag tag : tagDescription.getTags()) {
                    LOGGER.debug(String.format("Adding tag %s = %s to resource %s",
                            tag.getKey(), tag.getValue(), resource.getId()));
                    resource.setTag(tag.getKey(), tag.getValue());
                }
            }
        }

        Map<String, List<String>> elbtoASGMap = buildELBtoASGMap();
        for(Resource resource : resources) {
            List<String> asgList = elbtoASGMap.get(resource.getId());
            if (asgList != null && asgList.size() > 0) {
                resource.setAdditionalField("referencedASGCount", "" + asgList.size());
                String asgStr = StringUtils.join(asgList,",");
                resource.setDescription(resource.getDescription() + ", ASGS=" + asgStr);
                LOGGER.debug(String.format("Resource ELB %s is referenced by ASGs %s", resource.getId(), asgStr));
            } else {
                resource.setAdditionalField("referencedASGCount", "0");
                resource.setDescription(resource.getDescription() + ", ASGS=none");
                LOGGER.debug(String.format("No ASGs found for ELB %s", resource.getId()));
            }
        }

        return resources;
    }

    private Map<String, List<String>> buildELBtoASGMap() {
        AWSClient awsClient = getAWSClient();
        LOGGER.info(String.format("Getting all ELBs associated with ASGs in region %s", awsClient.region()));

        List<AutoScalingGroup> autoScalingGroupList = awsClient.describeAutoScalingGroups();
        HashMap<String, List<String>> asgMap = new HashMap<>();
        for (AutoScalingGroup asg : autoScalingGroupList) {
            String asgName = asg.getAutoScalingGroupName();
            if (asg.getLoadBalancerNames() != null ) {
                for (String elbName : asg.getLoadBalancerNames()) {
                    List<String> asgList = asgMap.get(elbName);
                    if (asgList == null) {
                        asgList = new ArrayList<>();
                        asgMap.put(elbName, asgList);
                    }
                    asgList.add(asgName);
                    LOGGER.debug(String.format("Found ASG %s associated with ELB %s", asgName, elbName));
                }
            }
        }
        return asgMap;
    }

}

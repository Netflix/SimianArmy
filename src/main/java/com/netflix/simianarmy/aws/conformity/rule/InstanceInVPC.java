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
package com.netflix.simianarmy.aws.conformity.rule;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ec2.model.Instance;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.conformity.AutoScalingGroup;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.Conformity;
import com.netflix.simianarmy.conformity.ConformityRule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The class implements a conformity rule to check an instance is in a virtual private cloud.
 */
public class InstanceInVPC implements ConformityRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceInVPC.class);

    private final Map<String, AWSClient> regionToAwsClient = Maps.newHashMap();

    private AWSCredentialsProvider awsCredentialsProvider;

    private static final String RULE_NAME = "InstanceInVPC";
    private static final String REASON = "VPC_ID not defined";

    /**
     * Constructs an instance with the default AWS credentials provider chain.
     * @see com.amazonaws.auth.DefaultAWSCredentialsProviderChain
     */
    public InstanceInVPC() {
        this(new DefaultAWSCredentialsProviderChain());
    }

    /**
     * Constructs an instance with the passed AWS credentials provider.
     * @param awsCredentialsProvider
     *      The AWS credentials provider
     */
    public InstanceInVPC(AWSCredentialsProvider awsCredentialsProvider) {
        this.awsCredentialsProvider = awsCredentialsProvider;
    }

    @Override
    public Conformity check(Cluster cluster) {
        Collection<String> failedComponents = Lists.newArrayList();
        //check all instances
        Set<String> failedInstances = checkInstancesInVPC(cluster.getRegion(), cluster.getSoloInstances());
        failedComponents.addAll(failedInstances);
        //check asg instances
        for (AutoScalingGroup asg : cluster.getAutoScalingGroups()) {
            if (asg.isSuspended()) {
                continue;
            }
            Set<String> asgFailedInstances = checkInstancesInVPC(cluster.getRegion(), asg.getInstances());
            failedComponents.addAll(asgFailedInstances);
        }
        return new Conformity(getName(), failedComponents);
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

    @Override
    public String getNonconformingReason() {
        return REASON;
    }

    private AWSClient getAwsClient(String region) {
        AWSClient awsClient = regionToAwsClient.get(region);
        if (awsClient == null) {
            awsClient = new AWSClient(region, awsCredentialsProvider);
            regionToAwsClient.put(region, awsClient);
        }
        return awsClient;
    }

    private Set<String> checkInstancesInVPC(String region, Collection<String> instances) {
        Set<String> failedInstances = Sets.newHashSet();
        for (String instanceId : instances) {
            for (Instance awsInstance : getAWSInstances(region, instanceId)) {
                if (awsInstance.getVpcId() == null) {
                    LOGGER.info(String.format("Instance %s is not in a virtual private cloud", instanceId));
                    failedInstances.add(instanceId);
                }
            }
        }
        return failedInstances;
    }

    /**
     * Gets the list of AWS instances. Can be overridden
     * @param region the region
     * @param instanceId the instance id.
     * @return the list of the AWS instances with the given id.
     */
    protected List<Instance> getAWSInstances(String region, String instanceId) {
        AWSClient awsClient = getAwsClient(region);
        return awsClient.describeInstances(instanceId);
    }
}
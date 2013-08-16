/*
 *
 *  Copyright 2013 Netflix, Inc.
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
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.conformity.AutoScalingGroup;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.Conformity;
import com.netflix.simianarmy.conformity.ConformityRule;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The class implementing a conformity rule that checks if there are instances that are older than certain days.
 * Instances are not considered to be permanent in the cloud, so sometimes having too old instances could indicate
 * potential issues.
 */
public class InstanceTooOld implements ConformityRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceHasStatusUrl.class);

    private static final String RULE_NAME = "InstanceTooOld";
    private final String reason;
    private final int instanceAgeThreshold;

    private AWSCredentialsProvider awsCredentialsProvider;

    /**
     * Constructor.
     * @param instanceAgeThreshold
     *      The age in days that makes an instance be considered too old.
     */
    public InstanceTooOld(int instanceAgeThreshold) {
        this(new DefaultAWSCredentialsProviderChain(), instanceAgeThreshold);
    }

    /**
     * Constructor.
     * @param awsCredentialsProvider
     *      The AWS credentials provider
     * @param instanceAgeThreshold
     *      The age in days that makes an instance be considered too old.
     */
    public InstanceTooOld(AWSCredentialsProvider awsCredentialsProvider, int instanceAgeThreshold) {
        this.awsCredentialsProvider = awsCredentialsProvider;
        Validate.isTrue(instanceAgeThreshold > 0);
        this.instanceAgeThreshold = instanceAgeThreshold;
        this.reason = String.format("Instances are older than %d days", instanceAgeThreshold);
    }

    @Override
    public Conformity check(Cluster cluster) {
        List<String> instanceIds = Lists.newArrayList();
        for (AutoScalingGroup asg : cluster.getAutoScalingGroups()) {
            instanceIds.addAll(asg.getInstances());
        }
        Map<String, Long> instanceIdToLaunchTime = getInstanceLaunchTimes(
                cluster.getRegion(), instanceIds.toArray(new String[instanceIds.size()]));

        Collection<String> failedComponents = Lists.newArrayList();
        long creationTimeThreshold = DateTime.now().minusDays(instanceAgeThreshold).getMillis();
        for (Map.Entry<String, Long> entry : instanceIdToLaunchTime.entrySet()) {
            String instanceId = entry.getKey();
            if (creationTimeThreshold > entry.getValue()) {
                LOGGER.info(String.format("Instance %s was created more than %d days ago",
                        instanceId, instanceAgeThreshold));
                failedComponents.add(instanceId);
            }
        }
        return new Conformity(getName(), failedComponents);
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

    @Override
    public String getNonconformingReason() {
        return reason;
    }

    /**
     * Gets the launch time (in milliseconds) for a list of instance ids of the same region. The default
     * implementation is using an AWS client. The method can be overridden in subclasses to get the instance
     * launch times differently.
     * @param region
     *      the region of the instances
     * @param instanceIds
     *      the instance ids, all instances should be in the same region.
     * @return
     *      the map from instance id to the launch time in milliseconds
     */
    protected Map<String, Long> getInstanceLaunchTimes(String region, String... instanceIds) {
        Map<String, Long> result = Maps.newHashMap();
        if (instanceIds == null || instanceIds.length == 0) {
            return result;
        }
        AWSClient awsClient = new AWSClient(region, awsCredentialsProvider);
        for (Instance instance : awsClient.describeInstances(instanceIds)) {
            if (instance.getLaunchTime() != null) {
                result.put(instance.getInstanceId(), instance.getLaunchTime().getTime());
            } else {
                LOGGER.warn(String.format("No launch time found for instance %s", instance.getInstanceId()));
            }
        }
        return result;
    }
}

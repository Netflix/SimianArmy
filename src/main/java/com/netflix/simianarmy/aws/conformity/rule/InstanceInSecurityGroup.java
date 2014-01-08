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
import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.conformity.AutoScalingGroup;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.Conformity;
import com.netflix.simianarmy.conformity.ConformityRule;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The class implementing a conformity rule that checks whether or not all instances in a cluster are in
 * specific security groups.
 */
public class InstanceInSecurityGroup implements ConformityRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceHasStatusUrl.class);

    private static final String RULE_NAME = "InstanceInSecurityGroup";
    private final String reason;

    private final Collection<String> requiredSecurityGroupNames = Sets.newHashSet();

    private AWSCredentialsProvider awsCredentialsProvider;

    /**
     * Constructor.
     * @param requiredSecurityGroupNames
     *      The security group names that are required to have for every instance of a cluster.
     */
    public InstanceInSecurityGroup(String... requiredSecurityGroupNames) {
        this(new DefaultAWSCredentialsProviderChain(), requiredSecurityGroupNames);
    }

    /**
     * Constructor.
     * @param awsCredentialsProvider
     *      The AWS credentials provider
     * @param requiredSecurityGroupNames
     *      The security group names that are required to have for every instance of a cluster.
     */
    public InstanceInSecurityGroup(AWSCredentialsProvider awsCredentialsProvider, String... requiredSecurityGroupNames)
    {
        this.awsCredentialsProvider = awsCredentialsProvider;
        Validate.notNull(requiredSecurityGroupNames);
        for (String sgName : requiredSecurityGroupNames) {
            Validate.notNull(sgName);
            this.requiredSecurityGroupNames.add(sgName.trim());
        }
        this.reason = String.format("Instances are not part of security groups (%s)",
                StringUtils.join(this.requiredSecurityGroupNames, ","));
    }

    @Override
    public Conformity check(Cluster cluster) {
        List<String> instanceIds = Lists.newArrayList();
        for (AutoScalingGroup asg : cluster.getAutoScalingGroups()) {
            instanceIds.addAll(asg.getInstances());
        }
        Collection<String> failedComponents = Lists.newArrayList();
        if (instanceIds.size() != 0) {
            Map<String, List<String>> instanceIdToSecurityGroup = getInstanceSecurityGroups(
                    cluster.getRegion(), instanceIds.toArray(new String[instanceIds.size()]));

            for (Map.Entry<String, List<String>> entry : instanceIdToSecurityGroup.entrySet()) {
                String instanceId = entry.getKey();
                if (!checkSecurityGroups(entry.getValue())) {
                    LOGGER.info(String.format("Instance %s does not have all required security groups", instanceId));
                    failedComponents.add(instanceId);
                }
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
     * Checks whether the collection of security group names are valid. The default implementation here is to check
     * whether the security groups contain the required security groups. The method can be overridden for different
     * rules.
     * @param sgNames
     *      The collection of security group names
     * @return
     *      true if the security group names are valid, false otherwise.
     */
    protected boolean checkSecurityGroups(Collection<String> sgNames) {
        for (String requiredSg : requiredSecurityGroupNames) {
            if (!sgNames.contains(requiredSg)) {
                LOGGER.info(String.format("Required security group %s is not found.", requiredSg));
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the security groups for a list of instance ids of the same region. The default implementation
     * is using an AWS client. The method can be overridden in subclasses to get the security groups differently.
     * @param region
     *      the region of the instances
     * @param instanceIds
     *      the instance ids, all instances should be in the same region.
     * @return
     *      the map from instance id to the list of security group names the instance has
     */
    protected Map<String, List<String>> getInstanceSecurityGroups(String region, String... instanceIds) {
        Map<String, List<String>> result = Maps.newHashMap();
        if (instanceIds == null || instanceIds.length == 0) {
            return result;
        }
        AWSClient awsClient = new AWSClient(region, awsCredentialsProvider);
        for (Instance instance : awsClient.describeInstances(instanceIds)) {
            // Ignore instances that are in VPC
            if (StringUtils.isNotEmpty(instance.getVpcId())) {
                LOGGER.info(String.format("Instance %s is in VPC and is ignored.", instance.getInstanceId()));
                continue;
            }

            if (!"running".equals(instance.getState().getName())) {
                LOGGER.info(String.format("Instance %s is not running, state is %s.",
                        instance.getInstanceId(), instance.getState().getName()));
                continue;
            }

            List<String> sgs = Lists.newArrayList();
            for (GroupIdentifier groupId : instance.getSecurityGroups()) {
                sgs.add(groupId.getGroupName());
            }
            result.put(instance.getInstanceId(), sgs);
        }
        return result;
    }
}

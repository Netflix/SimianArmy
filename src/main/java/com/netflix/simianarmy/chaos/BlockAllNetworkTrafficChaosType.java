/*
 *
 *  Copyright 2013 Justin Santa Barbara.
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
package com.netflix.simianarmy.chaos;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Blocks network traffic to/from instance, so it is running but offline.
 *
 * We actually put the instance into a different security group. First, because AWS requires a SG for some reason.
 * Second, because you might well want to continue to allow e.g. SSH inbound.
 */
public class BlockAllNetworkTrafficChaosType extends ChaosType {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BlockAllNetworkTrafficChaosType.class);

    private final String blockedSecurityGroupName;

    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     */
    public BlockAllNetworkTrafficChaosType(MonkeyConfiguration config) {
        super(config, "BlockAllNetworkTraffic");

        this.blockedSecurityGroupName = config.getStrOrElse(getConfigurationPrefix() + "group", "blocked-network");
    }

    /**
     * We can apply the strategy iff the blocked security group is configured.
     */
    @Override
    public boolean canApply(ChaosInstance instance) {
        CloudClient cloudClient = instance.getCloudClient();
        String instanceId = instance.getInstanceId();

        if (!cloudClient.canChangeInstanceSecurityGroups(instanceId)) {
            LOGGER.info("Not a VPC instance, can't change security groups");
            return false;
        }

        return super.canApply(instance);
    }

    /**
     * Takes the instance off the network.
     */
    @Override
    public void apply(ChaosInstance instance) {
        CloudClient cloudClient = instance.getCloudClient();
        String instanceId = instance.getInstanceId();

        if (!cloudClient.canChangeInstanceSecurityGroups(instanceId)) {
            throw new IllegalStateException("canApply should have returned false");
        }

        String groupId = cloudClient.findSecurityGroup(instance.getInstanceId(), blockedSecurityGroupName);

        if (groupId == null) {
            LOGGER.info("Auto-creating security group {}", blockedSecurityGroupName);

            String description = "Empty security group for blocked instances";
            groupId = cloudClient.createSecurityGroup(instance.getInstanceId(), blockedSecurityGroupName, description);
        }

        LOGGER.info("Blocking network traffic by applying security group {} to instance {}", groupId, instanceId);

        List<String> groups = Lists.newArrayList();
        groups.add(groupId);
        cloudClient.setInstanceSecurityGroups(instanceId, groups);
    }
}

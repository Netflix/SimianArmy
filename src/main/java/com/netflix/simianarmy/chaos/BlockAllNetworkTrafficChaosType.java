package com.netflix.simianarmy.chaos;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.SecurityGroup;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.client.aws.AWSClient;

/**
 * Blocks network traffic to/from instance, so it is running but offline.
 *
 * We actually put the instance into a different security group. First, because AWS requires a SG for some reason.
 * Second, because you might well want to continue to allow e.g. SSH inbound.
 */
public class BlockAllNetworkTrafficChaosType extends ChaosType {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicChaosMonkey.class);

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
    public boolean canApply(CloudClient cloudClient, String instanceId) {
        if (blockedSecurityGroupName == null) {
            LOGGER.debug("Can't apply strategy: security group not configured");
            return false;
        }
        if (cloudClient instanceof AWSClient) {
            LOGGER.debug("Not an AWSClient, can't use security groups");
            return false;
        }
        return super.canApply(cloudClient, instanceId);
    }

    /**
     * Takes the instance off the network.
     */
    @Override
    public void apply(CloudClient cloudClient, String instanceId) {
        if (!(cloudClient instanceof AWSClient)) {
            throw new IllegalStateException("canApply should have returned false");
        }

        String groupId;

        AWSClient awsClient = (AWSClient) cloudClient;
        List<SecurityGroup> securityGroups = awsClient.describeSecurityGroups(blockedSecurityGroupName);
        if (securityGroups.isEmpty()) {
            String description = "Empty security group for blocked instances";
            groupId = awsClient.createSecurityGroup(blockedSecurityGroupName, description);
        } else {
            SecurityGroup securityGroup = Iterables.getOnlyElement(securityGroups);
            groupId = securityGroup.getGroupId();
        }

        List<String> groups = Lists.newArrayList();
        groups.add(groupId);
        awsClient.setInstanceSecurityGroups(instanceId, groups);
    }
}

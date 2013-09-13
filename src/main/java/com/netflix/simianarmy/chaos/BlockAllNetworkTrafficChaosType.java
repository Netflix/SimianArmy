package com.netflix.simianarmy.chaos;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;

/**
 * Blocks network traffic to/from instance, so it is running but offline.
 *
 * We actually put the instance into a different security group. First, because
 * AWS requires a SG for some reason. Second, because you might well want to
 * continue to allow e.g. SSH inbound.
 */
public class BlockAllNetworkTrafficChaosType extends ChaosType {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicChaosMonkey.class);

    private final String blockedSecurityGroup;

    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     */
    public BlockAllNetworkTrafficChaosType(MonkeyConfiguration config) {
        super("BlockAllNetworkTraffic");

        this.blockedSecurityGroup = config.getStr("simianarmy.chaos.blockallnetworktraffic.group");
    }

    /**
     * We can apply the strategy iff the blocked security group is configured.
     */
    @Override
    public boolean canApply(CloudClient cloudClient, String instanceId) {
        if (blockedSecurityGroup == null) {
            LOGGER.debug("Can't apply strategy: security group not configured");
            return false;
        }
        return true;
    }

    /**
     * Takes the instance off the network.
     */
    @Override
    public void apply(CloudClient cloudClient, String instanceId) {
        List<String> groups = Lists.newArrayList();
        groups.add(blockedSecurityGroup);
        cloudClient.setSecurityGroups(instanceId, groups);
    }

}

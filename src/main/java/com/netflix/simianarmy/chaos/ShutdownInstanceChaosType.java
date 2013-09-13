package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Shuts down the instance using the cloud instance-termination API.
 *
 * This is the classic chaos-monkey strategy.
 */
public class ShutdownInstanceChaosType extends ChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     */
    public ShutdownInstanceChaosType(MonkeyConfiguration config) {
        super(config, "ShutdownInstance");
    }

    /**
     * Shuts down the instance.
     */
    @Override
    public void apply(CloudClient cloudClient, String instanceId) {
        cloudClient.terminateInstance(instanceId);
    }

}

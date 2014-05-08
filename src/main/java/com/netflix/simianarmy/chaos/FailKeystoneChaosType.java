package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Fail all Keystone API endpoints for Openstack.
 */
public class FailKeystoneChaosType extends FailOpenstackEndpointChaosType {

    /**
     * Get the right end point across to FailOpenstackEndpointChaosType.
     *
     * @param config
     *            Configuration settings
     */
    public FailKeystoneChaosType(final MonkeyConfiguration config) {
        super(config, "nova");
    }
}


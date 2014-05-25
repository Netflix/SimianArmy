package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Fail all Glance API endpoints for Openstack.
 */
public class FailGlanceChaosType extends FailOpenstackEndpointChaosType {

    /**
     * Get the right end point across to FailOpenstackEndpointChaosType.
     *
     * @param config
     *            Configuration settings
     */
    public FailGlanceChaosType(final MonkeyConfiguration config) {
        super(config, "glance");
    }
}


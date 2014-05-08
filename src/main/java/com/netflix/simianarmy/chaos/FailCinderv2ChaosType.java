package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Fail all Cinder v2.0 API endpoints for Openstack.
 */
public class FailCinderv2ChaosType extends FailOpenstackEndpointChaosType {

    /**
     * Get the right end point across to FailOpenstackEndpointChaosType.
     *
     * @param config
     *            Configuration settings
     */
    public FailCinderv2ChaosType(final MonkeyConfiguration config) {
        super(config, "cinderv2");
    }
}


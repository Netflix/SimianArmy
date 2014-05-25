package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Fail all Cinder API endpoints for Openstack.
 */
public class FailCinderChaosType extends FailOpenstackEndpointChaosType {

    /**
     * Get the right end point across to FailOpenstackEndpointChaosType.
     *
     * @param config
     *            Configuration settings
     */
    public FailCinderChaosType(final MonkeyConfiguration config) {
        super(config, "cinder");
    }
}


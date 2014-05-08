package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Fail all Nova API endpoints for Openstack.
 */
public class FailNovaChaosType extends FailOpenstackEndpointChaosType {

    /**
     * Get the right end point across to FailOpenstackEndpointChaosType.
     *
     * @param config
     *            Configuration settings
     */
    public FailNovaChaosType(final MonkeyConfiguration config) {
        super(config, "nova");
    }
}


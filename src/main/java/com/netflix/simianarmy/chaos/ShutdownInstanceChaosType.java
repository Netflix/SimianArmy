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
    public void apply(ChaosInstance instance) {
        CloudClient cloudClient = instance.getCloudClient();
        String instanceId = instance.getInstanceId();

        cloudClient.terminateInstance(instanceId);
    }

    /**
     * We want to default to enabled.
     */
    @Override
    protected boolean getEnabledDefault() {
        return true;
    }

}

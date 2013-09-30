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

import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;

/**
 * We force-detach all the EBS volumes.
 *
 * This is supposed to simulate a catastrophic failure of EBS, however the instance will (possibly) still keep running;
 * e.g. it should continue to respond to pings.
 */
public class DetachVolumesChaosType extends ChaosType {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicChaosMonkey.class);

    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     */
    public DetachVolumesChaosType(MonkeyConfiguration config) {
        super(config, "DetachVolumes");
    }

    /**
     * Strategy can be applied iff there are any EBS volumes attached.
     */
    @Override
    public boolean canApply(ChaosInstance instance) {
        CloudClient cloudClient = instance.getCloudClient();
        String instanceId = instance.getInstanceId();

        List<String> volumes = cloudClient.listAttachedVolumes(instanceId, false);
        if (volumes.isEmpty()) {
            LOGGER.debug("Can't apply strategy: no non-root EBS volumes");
            return false;
        }

        return super.canApply(instance);
    }

    /**
     * Force-detaches all attached EBS volumes from the instance.
     */
    @Override
    public void apply(ChaosInstance instance) {
        CloudClient cloudClient = instance.getCloudClient();
        String instanceId = instance.getInstanceId();

        // IDEA: We could have a strategy where we detach some of the volumes...
        boolean force = true;
        for (String volumeId : cloudClient.listAttachedVolumes(instanceId, false)) {
            cloudClient.detachVolume(instanceId, volumeId, force);
        }
    }

}

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Creates a huge file on the root device so that the disk fills up.
 */
public class FillDiskChaosType extends ScriptChaosType {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FillDiskChaosType.class);

    /**
     * Enhancement: As with BurnIoChaosType, it would be nice to randomize the volume.
     *
     * coryb suggested this, and proposed this script:
     *
     * nohup dd if=/dev/urandom of=/burn bs=1M count=$(df -ml /burn  | awk '/\//{print $2}') iflag=fullblock &
     */

    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     */
    public FillDiskChaosType(MonkeyConfiguration config) {
        super(config, "FillDisk");
    }

    @Override
    public boolean canApply(ChaosInstance instance) {
        if (!super.canApply(instance)) {
            return false;
        }

        if (isRootVolumeEbs(instance) && !isBurnMoneyEnabled()) {
            LOGGER.debug("Root volume is EBS so FillDisk would cost money; skipping");
            return false;
        }

        return true;
    }
}

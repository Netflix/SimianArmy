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
 * Executes a disk I/O intensive program on the node, reducing I/O capacity.
 *
 * This simulates either a noisy neighbor on the box or just a general issue with the disk.
 */
public class BurnIoChaosType extends ScriptChaosType {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BurnIoChaosType.class);

    /**
     * Enhancement: It would be nice to target other devices than the root disk.
     *
     * Considerations:
     * 1) EBS activity costs money.
     * 2) The root may be on EBS anyway.
     * 3) If it's costing money, we might want to stop after a while to stop runaway charges.
     *
     * coryb suggested this, and proposed something like this:
     *
     * tmp=$(mktemp)
     * df -hl -x tmpfs | awk '/\//{print $6}' > $tmp
     * mount=$(sed -n $((RANDOM%$(wc -l < $tmp)+1))p $tmp)
     * rm $tmp
     *
     * And then of=$mount/burn
     *
     * An alternative might be to run df over SSH, parse it here, and then pass the desired
     * path to the script.  This keeps the script simpler.  I don't think there's an easy way
     * to tell the difference between an EBS volume and an instance volume other than from the
     * EC2 API.
     */

    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     */
    public BurnIoChaosType(MonkeyConfiguration config) {
        super(config, "BurnIO");
    }

    @Override
    public boolean canApply(ChaosInstance instance) {
        if (!super.canApply(instance)) {
            return false;
        }

        if (isRootVolumeEbs(instance) && !isBurnMoneyEnabled()) {
            LOGGER.debug("Root volume is EBS so BurnIO would cost money; skipping");
            return false;
        }

        return true;
    }
}

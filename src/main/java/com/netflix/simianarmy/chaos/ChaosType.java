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

/**
 * A strategy pattern for different types of chaos the chaos monkey can cause.
 */
public abstract class ChaosType {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ChaosType.class);

    /**
     * Configuration for this chaos type.
     */
    private final MonkeyConfiguration config;

    /**
     * The unique key for the ChaosType.
     */
    private final String key;

    /**
     * Is this strategy enabled?
     */
    private final boolean enabled;

    /**
     * Protected constructor (abstract class).
     *
     * @param config
     *            Configuration to use
     * @param key
     *            Unique key for the ChaosType strategy
     */
    protected ChaosType(MonkeyConfiguration config, String key) {
        this.config = config;
        this.key = key;
        this.enabled = config.getBoolOrElse(getConfigurationPrefix() + "enabled", getEnabledDefault());

        LOGGER.info("ChaosType: {}: enabled={}", key, enabled);
    }

    /**
     * If not specified, controls whether we default to enabled.
     *
     * Most ChaosTypes should be disabled by default, not least for legacy compatibility, but we want at least one
     * strategy to be available.
     */
    protected boolean getEnabledDefault() {
        return false;
    }

    /**
     * Returns the configuration key prefix to use for this strategy.
     */
    protected String getConfigurationPrefix() {
        return "simianarmy.chaos." + key.toLowerCase() + ".";
    }

    /**
     * Returns the unique key for the ChaosType.
     */
    public String getKey() {
        return key;
    }

    /**
     * Checks if this chaos type can be applied to the given instance.
     *
     * For example, if the strategy was to detach all the EBS volumes, that only makes sense if there are EBS volumes to
     * detach.
     */
    public boolean canApply(ChaosInstance instance) {
        return isEnabled();
    }

    /**
     * Returns whether we are enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Applies this chaos type to the specified instance.
     */
    public abstract void apply(ChaosInstance instance);

    /**
     * Returns the ChaosType with the matching key.
     */
    public static ChaosType parse(List<ChaosType> all, String chaosTypeName) {
        for (ChaosType chaosType : all) {
            if (chaosType.getKey().equalsIgnoreCase(chaosTypeName)) {
                return chaosType;
            }
        }
        throw new IllegalArgumentException("Unknown chaos type value: "
                + chaosTypeName);
    }

    /**
     * Returns whether chaos types that cost money are allowed.
     */
    protected boolean isBurnMoneyEnabled() {
        return config.getBoolOrElse("simianarmy.chaos.burnmoney", false);
    }

    /**
     * Checks whether the root volume of the specified instance is on EBS.
     *
     * @param instance id of instance
     * @return true iff root is on EBS
     */
    protected boolean isRootVolumeEbs(ChaosInstance instance) {
        CloudClient cloudClient = instance.getCloudClient();
        String instanceId = instance.getInstanceId();

        List<String> withRoot = cloudClient.listAttachedVolumes(instanceId, true);
        List<String> withoutRoot = cloudClient.listAttachedVolumes(instanceId, false);

        return (withRoot.size() != withoutRoot.size());
    }
}

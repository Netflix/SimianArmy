package com.netflix.simianarmy.chaos;

import java.util.List;

import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * A strategy pattern for different types of chaos the chaos monkey can cause.
 */
public abstract class ChaosType {
    /**
     * Configuration for this chaos type
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
    public boolean canApply(CloudClient cloudClient, String instanceId) {
        return enabled;
    }

    /**
     * Applies this chaos type to the specified instance.
     */
    public abstract void apply(CloudClient cloudClient, String instanceId);

    /**
     * Returns the ChaosType with the matching key.
     */
    public static ChaosType parse(List<ChaosType> all, String chaosTypeName) {
        for (ChaosType chaosType : all) {
            if (chaosType.getKey().equalsIgnoreCase(chaosTypeName)) {
                return chaosType;
            }
        }
        throw new IllegalArgumentException("Unknown chaos type: " + chaosTypeName);
    }
}

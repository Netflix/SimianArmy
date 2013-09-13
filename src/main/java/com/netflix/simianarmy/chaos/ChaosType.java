package com.netflix.simianarmy.chaos;

import java.util.List;

import com.netflix.simianarmy.CloudClient;

/**
 * A strategy pattern for different types of chaos the chaos monkey can cause.
 */
public abstract class ChaosType {
    /**
     * The unique key for the ChaosType.
     */
    private final String key;

    /**
     * Protected constructor (abstract class).
     *
     * @param key
     *            Unique key for the ChaosType strategy
     */
    protected ChaosType(String key) {
        this.key = key;
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
     * For example, if the strategy was to detach all the EBS volumes, that only
     * makes sense if there are EBS volumes to detach.
     */
    public boolean canApply(CloudClient cloudClient, String instanceId) {
        return true;
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

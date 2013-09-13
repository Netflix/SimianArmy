package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.CloudClient;

/**
 * A strategy pattern for different types of chaos the chaos monkey can cause.
 */
public abstract class ChaosType {
    /**
     * All ChaosType patterns must be added to this array to be considered.
     */
    public static final ChaosType[] ALL = {ShutdownInstanceChaosType.INSTANCE,
            DetachVolumesChaosType.INSTANCE };

    /**
     * Default ChaosType to use if none specified.
     */
    public static final ChaosType DEFAULT = ShutdownInstanceChaosType.INSTANCE;

    /**
     * The unique key for the ChaosType.
     */
    final String key;

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
    public static ChaosType parse(String chaosTypeName) {
        for (ChaosType chaosType : ALL) {
            if (chaosType.getKey().equalsIgnoreCase(chaosTypeName)) {
                return chaosType;
            }
        }
        throw new IllegalArgumentException("Unknown chaos type value: "
                + chaosTypeName);
    }
}

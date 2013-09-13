package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.CloudClient;

/**
 * A strategy pattern for different types of chaos the chaos monkey can cause
 * 
 */
public abstract class ChaosType {
    final String key;

    public static ChaosType[] ALL = { ShutdownInstanceChaosType.INSTANCE,
            DetachVolumesChaosType.INSTANCE };

    public static final ChaosType DEFAULT = ShutdownInstanceChaosType.INSTANCE;

    protected ChaosType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    /**
     * Checks if this chaos type can be applied to the given instance.
     * 
     * For example, if the strategy was to detach all the EBS volumes, that only
     * makes sense if there are EBS volumes to detach.
     * 
     */
    public boolean canApply(CloudClient cloudClient, String instanceId) {
        return true;
    }

    /**
     * Applies this chaos type to the specified instance.
     * 
     */
    public abstract void apply(CloudClient cloudClient, String instanceId);

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

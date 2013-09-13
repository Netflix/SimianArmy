package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.CloudClient;

public class ShutdownInstanceChaosType extends ChaosType {

    public static final ChaosType INSTANCE = new ShutdownInstanceChaosType();

    protected ShutdownInstanceChaosType() {
        super("ShutdownInstance");
    }

    @Override
    public void apply(CloudClient cloudClient, String instanceId) {
        cloudClient.terminateInstance(instanceId);
    }

}

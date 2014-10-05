package com.netflix.simianarmy.client.vsphere;

import com.amazonaws.services.autoscaling.model.Instance;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The vSphere data type.
 * </p>
 */
public class VSphereGroup {

    VSphereGroup(String groupNme) {
        this.groupNme = groupNme;
        instances = new ArrayList<Instance>();
    }

    public String getGroupNme() {
        return groupNme;
    }

    private String groupNme;
    private java.util.List<Instance> instances;

    /**
     * Provides a summary list of vSphere instances.
     *
     * @return Provides a summary list of vSphere instances.
     */
    public List<Instance> getInstances() {
        return instances;
    }

    /**
     * Add vSphere instance to summary list.
     * @param instanceId vSphere instance id
     */
    public void addInstance(String instanceId) {
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instances.add(instance);
    }
}

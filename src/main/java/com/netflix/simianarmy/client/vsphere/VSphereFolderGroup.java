package com.netflix.simianarmy.client.vsphere;

import com.amazonaws.services.autoscaling.model.Instance;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The vSphere data type.
 * </p>
 */
public class VSphereFolderGroup {
    /**
     * vSphere absolute folder path
     */
    private String groupName;
    /**
     * A list of Instances which use Virtual Machine ID as instance ID.
     */
    private java.util.List<Instance> instances;

    public VSphereFolderGroup(String groupName) {
        this.groupName = groupName;
        instances = new ArrayList<Instance>();
    }

    public String getGroupName() {
        return groupName;
    }

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
     * @param instanceId Virtual Machine ID
     */
    public void addInstance(String instanceId) {
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instances.add(instance);
    }
}

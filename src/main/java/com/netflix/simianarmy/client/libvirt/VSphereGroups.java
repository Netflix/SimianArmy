package com.netflix.simianarmy.client.libvirt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;

class VSphereGroups {
    private static final Logger LOGGER = LoggerFactory.getLogger(VSphereGroups.class);
    
    final private Map<String, AutoScalingGroup> map;
    
    public VSphereGroups() {
        map = new HashMap<String, AutoScalingGroup>();
    }
    
    public List<AutoScalingGroup> asList() {
        return new ArrayList<AutoScalingGroup>(map.values());
    }
    
    public List<AutoScalingGroup> emptyList() {
        return new LinkedList<AutoScalingGroup>();
    }
    
    public void addInstance(final String instanceId, final String groupName) {
        LOGGER.debug("adding <{0}> to group <{1}>", instanceId, groupName);
    
        AutoScalingGroup asg = map.get(groupName);
        if (asg == null) {
            asg = new AutoScalingGroup();
            asg.setAutoScalingGroupName(groupName);
            map.put(groupName, asg);
        }
        List<Instance> instances = asg.getInstances();
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);
        instances.add(instance);
    }
}

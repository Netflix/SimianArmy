/*
 *  Copyright 2012 Immobilien Scout GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.simianarmy.client.vsphere;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;

/**
 * Wraps the creation and grouping of Instance's in AutoScalingGroup's.
 *
 * @author ingmar.krusch@immobilienscout24.de
 */
class VSphereGroups {
    private final Map<String, AutoScalingGroup> map = new HashMap<String, AutoScalingGroup>();

    /**
     * Get all AutoScalingGroup's that have been added.
     */
    public List<AutoScalingGroup> asList() {
        ArrayList<AutoScalingGroup> list = new ArrayList<AutoScalingGroup>(map.values());
        Collections.sort(list, new Comparator<AutoScalingGroup>() {
            @Override
            public int compare(AutoScalingGroup o1, AutoScalingGroup o2) {
                return o1.getAutoScalingGroupName().compareTo(o2.getAutoScalingGroupName());
            }
        });
        return list;
    }

    /**
     * Add the given instance to the named group.
     */
    public void addInstance(final String instanceId, final String groupName) {
        if (!map.containsKey(groupName)) {
            final AutoScalingGroup asg = new AutoScalingGroup();
            asg.setAutoScalingGroupName(groupName);
            map.put(groupName, asg);
        }

        final AutoScalingGroup asg = map.get(groupName);
        Instance instance = new Instance();
        instance.setInstanceId(instanceId);

        asg.getInstances().add(instance);
    }
}

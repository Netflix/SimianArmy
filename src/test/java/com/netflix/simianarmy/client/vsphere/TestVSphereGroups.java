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
// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.client.vsphere;

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
/**
 * @author ingmar.krusch@immobilienscout24.de
 */
public class TestVSphereGroups {
    @Test
    public void shouldReturnListContainigSingleASGWhenAddInstanceIsCalledOnce() {
        VSphereGroups groups = new VSphereGroups();
        groups.addInstance("anyInstanceId", "anyGroupName");
        List<AutoScalingGroup> list = groups.asList();

        assertEquals(1, list.size());

        AutoScalingGroup firstItem = list.get(0);
        assertEquals("anyGroupName", firstItem.getAutoScalingGroupName());

        List<Instance> instances = firstItem.getInstances();
        assertEquals(1, instances.size());

        assertEquals("anyInstanceId", instances.get(0).getInstanceId());
    }

    @Test
    public void shouldReturnListContainingSingleASGWithTwoInstancesWhenAddInstanceIsCaledTwiceForSameGroup() {
        VSphereGroups groups = new VSphereGroups();
        groups.addInstance("anyInstanceId", "anyGroupName");
        groups.addInstance("anyOtherInstanceId", "anyGroupName");
        List<AutoScalingGroup> list = groups.asList();

        assertEquals(1, list.size());

        List<Instance> instances = list.get(0).getInstances();
        assertEquals(2, instances.size());

        assertEquals("anyInstanceId", instances.get(0).getInstanceId());
        assertEquals("anyOtherInstanceId", instances.get(1).getInstanceId());
    }

    @Test
    public void shouldReturnListContainigTwoASGWhenAddInstanceIsCalledTwice() {
        VSphereGroups groups = new VSphereGroups();
        groups.addInstance("anyInstanceId", "anyGroupName");
        groups.addInstance("anyOtherInstanceId", "anyOtherGroupName");
        List<AutoScalingGroup> list = groups.asList();

        assertEquals(2, list.size());

        AutoScalingGroup firstGroup = list.get(0);
        assertEquals("anyGroupName", firstGroup.getAutoScalingGroupName());

        List<Instance> firstGroupInstances = firstGroup.getInstances();
        assertEquals(1, firstGroupInstances.size());

        assertEquals("anyInstanceId", firstGroupInstances.get(0).getInstanceId());

        AutoScalingGroup secondGroup = list.get(1);
        assertEquals("anyOtherGroupName", secondGroup.getAutoScalingGroupName());

        List<Instance> secondGroupInstances = secondGroup.getInstances();
        assertEquals(1, secondGroupInstances.size());

        assertEquals("anyOtherInstanceId", secondGroupInstances.get(0).getInstanceId());
    }
}

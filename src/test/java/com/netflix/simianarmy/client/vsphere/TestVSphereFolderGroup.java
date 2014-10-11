// CHECKSTYLE IGNORE Javadoc
/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.simianarmy.client.vsphere;

import com.amazonaws.services.autoscaling.model.Instance;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author xiaoy@vmware.com
 */
public class TestVSphereFolderGroup {
    @Test
    public void shouldReturnListContainigSingleVFGWhenAddInstanceIsCalledOnce() {
        VSphereFolderGroup group = new VSphereFolderGroup("DC/vApp1");
        group.addInstance("VM1");
        List<Instance> instances = group.getInstances();

        assertEquals(instances.size(), 1, "Instance Number");

        assertEquals(instances.get(0).getInstanceId(), "VM1", "InstanceId");
        assertEquals(group.getGroupName(), "DC/vApp1", "Group Name");
    }

    @Test
    public void shouldReturnListContainigTwoVFGWhenAddInstanceIsCalledTwice() {
        VSphereFolderGroup group = new VSphereFolderGroup("DC/vApp1");
        group.addInstance("VM1");
        group.addInstance("VM2");
        List<Instance> instances = group.getInstances();

        assertEquals(instances.size(), 2, "Instance Number");

        assertEquals(instances.get(0).getInstanceId(), "VM1", "InstanceId");
        assertEquals(instances.get(1).getInstanceId(), "VM2", "InstanceId");
        assertEquals(group.getGroupName(), "DC/vApp1", "Group Name");
    }

}

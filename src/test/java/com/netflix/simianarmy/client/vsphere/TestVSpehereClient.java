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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * @author ingmar.krusch@immobilienscout24.de
 */
public class TestVSpehereClient {
    @Test
    public void shouldTerminateCorrectly() throws RemoteException {
        VSphereServiceConnection connection = mock(VSphereServiceConnection.class);
        VirtualMachine vm1 = createVMMock("vm1");
        when(connection.getVirtualMachineById("vm1")).thenReturn(vm1);

        TerminationStrategy strategy = mock(PropertyBasedTerminationStrategy.class);

        VSphereClient client = new VSphereClient(strategy, connection);
        client.terminateInstance("vm1");

        verify(strategy, times(1)).terminate(vm1);
    }

    @Test
    public void shouldDescribeGroupsCorrectly() {
        VSphereServiceConnection connection = mock(VSphereServiceConnection.class);
        TerminationStrategy strategy = mock(PropertyBasedTerminationStrategy.class);
        VirtualMachine[] virtualMachines = {createVMMock("vm1"), createVMMock("vm2")};
        when(connection.describeVirtualMachines()).thenReturn(virtualMachines);

        VSphereClient client = new VSphereClient(strategy, connection);

        List<AutoScalingGroup> groups = client.describeAutoScalingGroups();
        String str = flattenAutoScallingGroups(groups);

        assertTrue(groups.size() == 2, "did not desribes the 2 vm's that were given");
        assertTrue(str.indexOf("group:vm1.parent.name:id:vm1.name:") >= 0, "did not describe vm1 correctly");
        assertTrue(str.indexOf("group:vm2.parent.name:id:vm2.name:") >= 0, "did not describe vm2 correctly");
    }

    @Test
    public void shouldDescribeVsphereGroupsCorrectly() {
        VSphereServiceConnection connectionMock = mock(VSphereServiceConnection.class);
        TerminationStrategy strategyMock = mock(PropertyBasedTerminationStrategy.class);

        String absolutePath1 = "DataCenter/Folder1/Folder";
        List<VirtualMachine> vmList1 = Arrays.asList(
                createVMMock("vm1", absolutePath1), createVMMock("vm2", absolutePath1));
        when(connectionMock.describeVirtualMachines(absolutePath1)).thenReturn(vmList1);

        String absolutePath2 = "DataCenter/Folder2/Folder";
        List<VirtualMachine> vmList2 = Arrays.asList(
                createVMMock("vm3", absolutePath2), createVMMock("vm4", absolutePath2));
        when(connectionMock.describeVirtualMachines(absolutePath2)).thenReturn(vmList2);

        VSphereClient client = new VSphereClient(strategyMock, connectionMock);

        List<VSphereFolderGroup> groups = client.describeVsphereGroups(new String[]{absolutePath1, absolutePath2});
        String str = flattenVSphereGroups(groups);

        assertTrue(groups.size() == 2, "did not desribes the 2 groups that were given");
        assertTrue(str.indexOf("group:DataCenter/Folder1/Folder:id:vm1.name:id:vm2.name:") >= 0,
                "did not describe vm1 and vm2 correctly");
        assertTrue(str.indexOf("group:DataCenter/Folder2/Folder:id:vm3.name:id:vm4.name:") >= 0,
                "did not describe vm3 and vm4 correctly");
    }

    private String flattenAutoScallingGroups(List<AutoScalingGroup> groups) {
        StringBuilder buf = new StringBuilder();
        for (AutoScalingGroup asg : groups) {
            List<Instance> instances = asg.getInstances();
            buf.append("group:").append(asg.getAutoScalingGroupName()).append(":");
            for (Instance instance : instances) {
                buf.append("id:").append(instance.getInstanceId()).append(":");
            }
        }
        return buf.toString();
    }

    private String flattenVSphereGroups(List<VSphereFolderGroup> groups) {
        StringBuilder buf = new StringBuilder();
        for (VSphereFolderGroup vfg : groups) {
            List<Instance> instances = vfg.getInstances();
            buf.append("group:").append(vfg.getGroupName()).append(":");
            for (Instance instance : instances) {
                buf.append("id:").append(instance.getInstanceId()).append(":");
            }
        }
        return buf.toString();
    }

    private VirtualMachine createVMMock(String id) {
        VirtualMachine vm1 = mock(VirtualMachine.class);
        ManagedEntity me1 = mock(ManagedEntity.class);
        when(vm1.getName()).thenReturn(id + ".name");
        when(vm1.getParent()).thenReturn(me1);
        when(me1.getName()).thenReturn(id + ".parent.name");
        return vm1;
    }

    private VirtualMachine createVMMock(String id, String parentName) {
        VirtualMachine vm1 = mock(VirtualMachine.class);
        ManagedEntity me1 = mock(ManagedEntity.class);
        when(vm1.getName()).thenReturn(id + ".name");
        when(vm1.getParent()).thenReturn(me1);
        when(me1.getName()).thenReturn(parentName);
        return vm1;
    }
}

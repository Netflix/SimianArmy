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

import static com.netflix.simianarmy.client.vsphere.VSphereServiceConnection.VIRTUAL_MACHINE_TYPE_NAME;
import static com.netflix.simianarmy.client.vsphere.VSphereServiceConnection.DC_TYPE_NAME;
import static junit.framework.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import java.rmi.RemoteException;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.amazonaws.AmazonServiceException;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.vmware.vim25.InvalidProperty;
import com.vmware.vim25.RuntimeFault;
import com.vmware.vim25.mo.InventoryNavigator;
import com.vmware.vim25.mo.ManagedEntity;
import com.vmware.vim25.mo.VirtualMachine;
import com.vmware.vim25.mo.Datacenter;
import com.vmware.vim25.mo.Folder;

/**
 * @author ingmar.krusch@immobilienscout24.de
 */
public class TestVSphereServiceConnection {
    // private ServiceInstance serviceMock = mock(ServiceInstance.class);
    private BasicConfiguration configMock = mock(BasicConfiguration.class);

    @Test
    public void shouldReturnConfiguredPropertiesAfterConstructedFromConfig() {
        when(configMock.getStr("simianarmy.client.vsphere.username")).thenReturn("configured username");
        when(configMock.getStr("simianarmy.client.vsphere.password")).thenReturn("configured password");
        when(configMock.getStr("simianarmy.client.vsphere.url")).thenReturn("configured url");

        VSphereServiceConnection service = new VSphereServiceConnection(configMock);

        assertEquals(service.getUsername(), "configured username");
        assertEquals(service.getPassword(), "configured password");
        assertEquals(service.getUrl(), "configured url");
    }

    @Test
    public void shouldCallSearchManagedEntityAndReturnVMForDoItGetVirtualMachineById()
                    throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                        new VSphereServiceConnectionWithMockedInventoryNavigator();
        InventoryNavigator inventoryNavigatorMock = service.getInventoryNavigatorMock();

        VirtualMachine vmMock = mock(VirtualMachine.class);
        when(inventoryNavigatorMock.searchManagedEntity(VIRTUAL_MACHINE_TYPE_NAME, "instanceId")).thenReturn(vmMock);

        VirtualMachine actualVM = service.getVirtualMachineById("instanceId");

        verify(inventoryNavigatorMock).searchManagedEntity(VIRTUAL_MACHINE_TYPE_NAME, "instanceId");
        assertSame(vmMock, actualVM);
    }

    @Test //(expectedExceptions = AmazonServiceException.class)
    public void shouldThrowExceptionWhenCallingSearchManagedEntitiesOnDescribeWhenNoVMsAreReturned()
                    throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                        new VSphereServiceConnectionWithMockedInventoryNavigator();

        try {
            service.describeVirtualMachines();
        } catch (AmazonServiceException e) {
            Assert.assertTrue(e != null);
        }
    }

    @Test
    public void shouldCallSearchManagedEntitiesOnDescribeWhenAtLeastOneVMIsReturned()
                    throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                        new VSphereServiceConnectionWithMockedInventoryNavigator();
        InventoryNavigator inventoryNavigatorMock = service.getInventoryNavigatorMock();

        ManagedEntity[] meMocks = new ManagedEntity[] {mock(VirtualMachine.class)};
        when(inventoryNavigatorMock.searchManagedEntities(VIRTUAL_MACHINE_TYPE_NAME)).thenReturn(meMocks);

        VirtualMachine[] actualVMs = service.describeVirtualMachines();

        verify(inventoryNavigatorMock).searchManagedEntities(VIRTUAL_MACHINE_TYPE_NAME);
        assertSame(meMocks[0], actualVMs[0]);
    }

    @Test
    public void shouldTraverseDataCenterAndFolderAtLeastOneVMIsReturned() throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                new VSphereServiceConnectionWithMockedInventoryNavigator();
        InventoryNavigator inventoryNavigatorMock = service.getInventoryNavigatorMock();

        Datacenter[] dcMocks = new Datacenter[] {mock(Datacenter.class)};
        Folder rootFolderMock = mock(Folder.class);
        Folder[] folderMocks = new Folder[] {mock(Folder.class), mock(Folder.class)};
        VirtualMachine[] vmMocks = new VirtualMachine[] {mock(VirtualMachine.class)};
        when(inventoryNavigatorMock.searchManagedEntities(DC_TYPE_NAME)).thenReturn(dcMocks);
        when(dcMocks[0].getName()).thenReturn("DC");
        when(dcMocks[0].getVmFolder()).thenReturn(rootFolderMock);
        when(rootFolderMock.getChildEntity()).thenReturn(folderMocks);
        when(folderMocks[0].getName()).thenReturn("Folder-001");
        when(folderMocks[1].getName()).thenReturn("Folder-002");
        when(folderMocks[1].getChildEntity()).thenReturn(vmMocks);
        List<VirtualMachine> actualVMs = service.describeVirtualMachines("DC/Folder-002");
        assertSame(vmMocks[0], actualVMs.get(0));
    }

    @Test(expectedExceptions = AmazonServiceException.class)
    public void shouldEncapsulateRemoteExceptionWhenGetVmFolder() throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                new VSphereServiceConnectionWithMockedInventoryNavigator();
        InventoryNavigator inventoryNavigatorMock = service.getInventoryNavigatorMock();

        Datacenter[] dcMocks = new Datacenter[] {mock(Datacenter.class)};
        when(inventoryNavigatorMock.searchManagedEntities(DC_TYPE_NAME)).thenReturn(dcMocks);
        when(dcMocks[0].getName()).thenReturn("DC");
        when(dcMocks[0].getVmFolder()).thenThrow(new RemoteException());
        service.describeVirtualMachines("DC");
    }

    @Test(expectedExceptions = AmazonServiceException.class)
    public void shouldEncapsulateInvalidPropertyExceptionWhenSearchManagedEntities() throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                new VSphereServiceConnectionWithMockedInventoryNavigator();
        InventoryNavigator inventoryNavigatorMock = service.getInventoryNavigatorMock();
        when(inventoryNavigatorMock.searchManagedEntities(DC_TYPE_NAME)).thenThrow(new InvalidProperty());

        service.searchManagedEntities(DC_TYPE_NAME);
    }

    @Test(expectedExceptions = AmazonServiceException.class)
    public void shouldEncapsulateIRuntimeFaultExceptionWhenSearchManagedEntities() throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                new VSphereServiceConnectionWithMockedInventoryNavigator();
        InventoryNavigator inventoryNavigatorMock = service.getInventoryNavigatorMock();
        when(inventoryNavigatorMock.searchManagedEntities(DC_TYPE_NAME)).thenThrow(new RuntimeFault());

        service.searchManagedEntities(DC_TYPE_NAME);
    }

    @Test(expectedExceptions = AmazonServiceException.class)
    public void shouldEncapsulateRemoteExceptionWhenSearchManagedEntities() throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                new VSphereServiceConnectionWithMockedInventoryNavigator();
        InventoryNavigator inventoryNavigatorMock = service.getInventoryNavigatorMock();
        when(inventoryNavigatorMock.searchManagedEntities(DC_TYPE_NAME)).thenThrow(new RemoteException());

        service.searchManagedEntities(DC_TYPE_NAME);
    }

    @Test(expectedExceptions = AmazonServiceException.class)
    public void shouldEncapsulateRemoteExceptionWhenGetChildEntity() throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                new VSphereServiceConnectionWithMockedInventoryNavigator();
        Folder parent = mock(Folder.class);
        when(parent.getChildEntity()).thenThrow(new RemoteException());

        service.getChildEntity(parent);
    }

    @Test(expectedExceptions = AmazonServiceException.class)
    public void shouldEncapsulateInvalidPropertyException() throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                        new VSphereServiceConnectionWithMockedInventoryNavigator();
        InventoryNavigator inventoryNavigatorMock = service.getInventoryNavigatorMock();
        when(inventoryNavigatorMock.searchManagedEntities(VIRTUAL_MACHINE_TYPE_NAME)).thenThrow(new InvalidProperty());

        service.describeVirtualMachines();
    }

    @Test(expectedExceptions = AmazonServiceException.class)
    public void shouldEncapsulateRuntimeFaultException() throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                        new VSphereServiceConnectionWithMockedInventoryNavigator();
        InventoryNavigator inventoryNavigatorMock = service.getInventoryNavigatorMock();
        when(inventoryNavigatorMock.searchManagedEntities(VIRTUAL_MACHINE_TYPE_NAME)).thenThrow(new RuntimeFault());

        service.describeVirtualMachines();
    }

    @Test(expectedExceptions = AmazonServiceException.class)
    public void shouldEncapsulateRemoteExceptionException() throws RemoteException {
        VSphereServiceConnectionWithMockedInventoryNavigator service =
                        new VSphereServiceConnectionWithMockedInventoryNavigator();
        InventoryNavigator inventoryNavigatorMock = service.getInventoryNavigatorMock();
        when(inventoryNavigatorMock.searchManagedEntities(VIRTUAL_MACHINE_TYPE_NAME)).thenThrow(new RemoteException());

        service.describeVirtualMachines();
    }

    // The API class ServerConnection is final and can therefore not be mocked.
    // It's possible to work around this using a wrapper, but this is a lot of
    // fake code that needs to be written and tested again just to test that
    // this code really calls the interface method. This is something that rather
    // should be tested in a system test.

    //@Test
    //    public void shouldDisconnectSeviceByLogoutOverConnection() {
    //        VSphereServiceConnectionWithMockedConnection connection =
    //            new VSphereServiceConnectionWithMockedConnection();
    //
    //        ServiceInstance serviceMock = connection.getService();
    //        ServerConnection serverConnectionMock = mock(ServerConnection.class);
    //        when(serviceMock.getServerConnection()).thenReturn(serverConnectionMock);
    //
    //        connection.disconnect();
    //
    //        verify(serviceMock).getServerConnection();
    //        verify(serverConnectionMock).logout();
    //        assertNull(connection.getService());
    //    }
}
//class VSphereServiceConnectionWithMockedConnection extends VSphereServiceConnection {
//    public VSphereServiceConnectionWithMockedConnection() {
//        super(mock(BasicConfiguration.class));
//        this.setService(mock(ServiceInstance.class));
//    }
//}

class VSphereServiceConnectionWithMockedInventoryNavigator extends VSphereServiceConnection {
    private InventoryNavigator inventoryNavigatorMock = mock(InventoryNavigator.class);

    public VSphereServiceConnectionWithMockedInventoryNavigator() {
        super(mock(BasicConfiguration.class));
    }

    @Override
    protected InventoryNavigator getInventoryNavigator() {
        return inventoryNavigatorMock;
    }

    public InventoryNavigator getInventoryNavigatorMock() {
        return inventoryNavigatorMock;
    }
}
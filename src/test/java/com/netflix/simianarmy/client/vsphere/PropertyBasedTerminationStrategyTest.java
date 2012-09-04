package com.netflix.simianarmy.client.vsphere;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.rmi.RemoteException;

import org.testng.annotations.Test;

import com.netflix.simianarmy.basic.BasicConfiguration;
import com.vmware.vim25.mo.VirtualMachine;

public class PropertyBasedTerminationStrategyTest {

    private BasicConfiguration configMock = mock(BasicConfiguration.class);
    private VirtualMachine virtualMachineMock = mock(VirtualMachine.class);
    
    @Test
    public void shouldReturnConfiguredPropertyNameAndValueAfterConstructedFromConfig() {
        when(configMock.getStrOrElse("client.vsphere.terminationStrategy.property.name", "Force Boot"))
            .thenReturn("configured name");
        when(configMock.getStrOrElse("client.vsphere.terminationStrategy.property.value", "server"))
            .thenReturn("configured value");
        
        PropertyBasedTerminationStrategy strategy = new PropertyBasedTerminationStrategy(configMock);
        
        assertEquals(strategy.getPropertyName(), "configured name");
        assertEquals(strategy.getPropertyValue(), "configured value");        
    }

    @Test
    public void shouldSetPropertyAndResetVirtualMachineAfterTermination() {
        when(configMock.getStrOrElse("client.vsphere.terminationStrategy.property.name", "Force Boot"))
            .thenReturn("configured name");
        when(configMock.getStrOrElse("client.vsphere.terminationStrategy.property.value", "server"))
            .thenReturn("configured value");

        PropertyBasedTerminationStrategy strategy = new PropertyBasedTerminationStrategy(configMock);
        
        try {
            strategy.terminate(virtualMachineMock);
            verify(virtualMachineMock, times(1)).setCustomValue("configured name", "configured value");
            verify(virtualMachineMock, times(1)).resetVM_Task();
        } catch (RemoteException e) {
            fail("termination should not fail", e);
        }
    }
}

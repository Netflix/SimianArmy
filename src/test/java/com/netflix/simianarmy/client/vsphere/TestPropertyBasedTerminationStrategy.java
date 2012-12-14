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
//CHECKSTYLE IGNORE Javadoc
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

/**
 * @author ingmar.krusch@immobilienscout24.de
 */
public class TestPropertyBasedTerminationStrategy {
    private BasicConfiguration configMock = mock(BasicConfiguration.class);
    private VirtualMachine virtualMachineMock = mock(VirtualMachine.class);

    @Test
    public void shouldReturnConfiguredPropertyNameAndValueAfterConstructedFromConfig() {
        when(configMock.getStrOrElse("simianarmy.client.vsphere.terminationStrategy.property.name", "Force Boot"))
            .thenReturn("configured name");
        when(configMock.getStrOrElse("simianarmy.client.vsphere.terminationStrategy.property.value", "server"))
            .thenReturn("configured value");

        PropertyBasedTerminationStrategy strategy = new PropertyBasedTerminationStrategy(configMock);

        assertEquals(strategy.getPropertyName(), "configured name");
        assertEquals(strategy.getPropertyValue(), "configured value");
    }

    @Test
    public void shouldSetPropertyAndResetVirtualMachineAfterTermination() {
        when(configMock.getStrOrElse("simianarmy.client.vsphere.terminationStrategy.property.name", "Force Boot"))
            .thenReturn("configured name");
        when(configMock.getStrOrElse("simianarmy.client.vsphere.terminationStrategy.property.value", "server"))
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

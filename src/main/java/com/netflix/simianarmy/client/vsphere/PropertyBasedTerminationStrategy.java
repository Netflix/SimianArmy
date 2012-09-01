package com.netflix.simianarmy.client.vsphere;

import java.rmi.RemoteException;

import com.netflix.simianarmy.basic.BasicConfiguration;
import com.vmware.vim25.mo.VirtualMachine;

/*
 *  Copyright 2012 Immobilienscout GmbH
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
 */
/**
 * Terminates a VirtualMachine by setting the named property and resetting it.
 *
 * @author ingmar.krusch@immobilienscout24.de
 */
public class PropertyBasedTerminationStrategy implements TerminationStrategy {

    private String propertyName;
    private String propertyValue;

    /**
     * Reads property name <code>client.vsphere.terminationStrategy.property.name</code> (default: Force Boot) and value
     * <code>client.vsphere.terminationStrategy.property.value</code> (default: server) from config.
     */
    public PropertyBasedTerminationStrategy(BasicConfiguration config) {
        this.propertyName = config.getStrOrElse("client.vsphere.terminationStrategy.property.name", "Force Boot");
        this.propertyValue = config.getStrOrElse("client.vsphere.terminationStrategy.property.value", "server");
    }

    @Override
    public void terminate(VirtualMachine virtualMachine) throws RemoteException {
        virtualMachine.setCustomValue(this.propertyName, this.propertyValue);
        virtualMachine.resetVM_Task();
    }
}

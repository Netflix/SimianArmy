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

import java.rmi.RemoteException;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * Terminates a VirtualMachine by setting the named property and resetting it.
 *
 * The following properties can be overridden in the client.properties
 * simianarmy.client.vsphere.terminationStrategy.property.name  = PROPERTY_NAME
 * simianarmy.client.vsphere.terminationStrategy.property.value = PROPERTY_VALUE
 *
 * @author ingmar.krusch@immobilienscout24.de
 */
public class PropertyBasedTerminationStrategy implements TerminationStrategy {
    private final String propertyName;
    private final String propertyValue;

    /**
     * Reads property name <code>simianarmy.client.vsphere.terminationStrategy.property.name</code>
     * (default: Force Boot) and value <code>simianarmy.client.vsphere.terminationStrategy.property.value</code>
     * (default: server) from config.
     */
    public PropertyBasedTerminationStrategy(MonkeyConfiguration config) {
        this.propertyName = config.getStrOrElse(
                "simianarmy.client.vsphere.terminationStrategy.property.name", "Force Boot");
        this.propertyValue = config.getStrOrElse(
                "simianarmy.client.vsphere.terminationStrategy.property.value", "server");
    }

    @Override
    public void terminate(VirtualMachine virtualMachine) throws RemoteException {
        virtualMachine.setCustomValue(getPropertyName(), getPropertyValue());
        virtualMachine.resetVM_Task();
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPropertyValue() {
        return propertyValue;
    }
}

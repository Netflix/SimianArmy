package com.netflix.simianarmy.client.vsphere;

import java.rmi.RemoteException;

import com.netflix.simianarmy.basic.BasicConfiguration;
import com.vmware.vim25.mo.VirtualMachine;

/**
 * Terminates a VirtualMachine by setting the named property and resetting it.
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

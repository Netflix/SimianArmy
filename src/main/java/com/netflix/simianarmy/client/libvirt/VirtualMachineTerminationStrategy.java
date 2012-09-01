package com.netflix.simianarmy.client.libvirt;

import java.rmi.RemoteException;

import com.vmware.vim25.mo.VirtualMachine;

/**
 * Abstracts the concrete way a VirtualMachine is terminated. Implement this to fit to your infrastructure.
 */
public interface VirtualMachineTerminationStrategy {
    /**
     * Terminate the given VirtualMachine.
     */
    void terminate(VirtualMachine virtualMachine) throws RemoteException;
}

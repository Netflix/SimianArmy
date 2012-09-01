package com.netflix.simianarmy.client.vsphere;

import java.rmi.RemoteException;

import com.vmware.vim25.mo.VirtualMachine;

/**
 * Abstracts the concrete way a VirtualMachine is terminated. Implement this to fit to your infrastructure.
 */
public interface TerminationStrategy {
    /**
     * Terminate the given VirtualMachine.
     */
    void terminate(VirtualMachine virtualMachine) throws RemoteException;
}

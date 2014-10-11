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
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.google.common.net.HostAndPort;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.vmware.vim25.mo.VirtualMachine;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.http.handlers.BackoffLimitedRetryHandler;
import org.jclouds.proxy.internal.GuiceProxyConfig;
import org.jclouds.ssh.SshClient;
import org.jclouds.ssh.jsch.JschSshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This client describes the VSphere folders as VSphereFolderGroup's containing the virtual machines that are directly in
 * that folder. And it can terminate these VMs with the configured TerminationStrategy as well as other SriptChaosType.
 *
 * @author ingmar.krusch@immobilienscout24.de
 */
public class VSphereClient extends AWSClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(VSphereClient.class);

    private final TerminationStrategy terminationStrategy;
    private final VSphereServiceConnection connection;

    /**
     * Create the specific Client from the given strategy and connection.
     */
    public VSphereClient(TerminationStrategy terminationStrategy, VSphereServiceConnection connection) {
        super("region-" + connection.getUrl());
        this.terminationStrategy = terminationStrategy;
        this.connection = connection;
    }

    @Override
    @Deprecated
    /**
     * This method describes the VSphere folders as AutoScalingGroup's containing the virtual machines that are directly
     * in that folder. The hierarchy is flattened this way. And it can terminate these VMs with the configured
     * TerminationStrategy.
     * One disadvantage is that different folders with the same name are welded into the same group.
     * Another is that it make chaos to those non-target applications.
     * @param names
     * @return Provided a summary list of groups.
     */
    public List<AutoScalingGroup> describeAutoScalingGroups(String... names) {
        final VSphereGroups groups = new VSphereGroups();

        try {
            connection.connect();

            for (VirtualMachine virtualMachine : connection.describeVirtualMachines()) {
                String instanceId = virtualMachine.getName();
                String groupName = virtualMachine.getParent().getName();

                boolean shouldAddNamedGroup = true;
                if (names != null) {
                    // TODO need to implement this feature!!!
                    throw new RuntimeException("This feature (selecting groups by name) is not implemented yet");
                }

                if (shouldAddNamedGroup) {
                    groups.addInstance(instanceId, groupName);
                }
            }
        } finally {
            connection.disconnect();
        }

        return groups.asList();
    }

    /**
     * Provided a summary list of groups. All virtual machines directly under the same folder will be welded into
     * a group.
     * @param names Each name should be a vSphere absolute folder path
     * @return Provided a summary list of groups.
     */
    public List<VSphereFolderGroup> describeVsphereGroups(String... names) {
        ArrayList<VSphereFolderGroup> groupList = new ArrayList<VSphereFolderGroup>();
        try {
            connection.connect();

            for (String name : names) {
                VSphereFolderGroup group = new VSphereFolderGroup(name);
                for (VirtualMachine virtualMachine : connection.describeVirtualMachines(name)) {
                    String instanceId = virtualMachine.getName();
                    LOGGER.info(instanceId);
                    group.addInstance(instanceId);
                }
                groupList.add(group);
            }

        } finally {
            connection.disconnect();
        }

        return groupList;
    }

    @Override
    /**
     * reinstall the given instance. If it is powered down this will be ignored and the
     * reinstall occurs the next time the machine is powered up.
     */
    public void terminateInstance(String instanceId) {
        try {
            connection.connect();

            VirtualMachine virtualMachine = connection.getVirtualMachineById(instanceId);
            this.terminationStrategy.terminate(virtualMachine);
        } catch (RemoteException e) {
            throw new AmazonServiceException("cannot destroy & recreate " + instanceId, e);
        } finally {
            connection.disconnect();
        }
    }

    @Override
    /**
     * Set up ssh connection to the virtual machine.  This is to make SriptChaosType to also run in vSphere.
     * @param instanceId Virtual machine ID
     * @param credentials SSH credential
     * @return SSH client
     */
    public SshClient connectSsh(String instanceId, LoginCredentials credentials) {
        String ipAddress = null;
        try {
            connection.connect();
            VirtualMachine virtualMachine = connection.getVirtualMachineById(instanceId);
            ipAddress = virtualMachine.getGuest().getIpAddress();
        } catch (RemoteException e) {
            throw new AmazonServiceException("cannot destroy & recreate " + instanceId, e);
        } finally {
            connection.disconnect();
        }
        JschSshClient ssh = new JschSshClient(new GuiceProxyConfig(), BackoffLimitedRetryHandler.INSTANCE, HostAndPort.fromParts(ipAddress, 22), credentials, 1*1000);
        ssh.connect();
        return ssh;
    }
}

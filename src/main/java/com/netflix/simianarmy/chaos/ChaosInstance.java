/*
 *
 *  Copyright 2013 Justin Santa Barbara.
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
 *
 */
package com.netflix.simianarmy.chaos;

import java.util.Set;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.Utils;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.netflix.simianarmy.CloudClient;

/**
 * Wrapper around an instance on which we are going to cause chaos.
 */
public class ChaosInstance {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ChaosInstance.class);

    final CloudClient cloudClient;
    final String instanceId;
    final SshConfig sshConfig;

    /**
     * Constructor.
     *
     * @param cloudClient
     *            client for cloud access
     * @param instanceId
     *            id of instance on cloud
     * @param sshConfig
     *            SSH configuration to access instance
     */
    public ChaosInstance(CloudClient cloudClient, String instanceId, SshConfig sshConfig) {
        this.cloudClient = cloudClient;
        this.instanceId = instanceId;
        this.sshConfig = sshConfig;
    }

    /**
     * Gets the {@link SshConfig} used to SSH to the instance
     *
     * @return the {@link SshConfig}
     */
    public SshConfig getSshConfig() {
        return sshConfig;
    }

    /**
     * Gets the {@link CloudClient} used to access the cloud
     *
     * @return the {@link CloudClient}
     */
    public CloudClient getCloudClient() {
        return cloudClient;
    }

    /**
     * Returns the instance id to identify the instance to the cloud client
     * 
     * @return instance id
     */
    public String getInstanceId() {
        return instanceId;
    }

    Boolean canConnectSsh = null;

    /**
     * Check if the SSH credentials are working.
     * 
     * This is cached for the duration of this object.
     * 
     * @return true iff ssh is configured and able to log on to instance.
     */
    public boolean canConnectSsh(ChaosInstance instance) {
        if (!sshConfig.isEnabled()) {
            return false;
        }

        if (canConnectSsh == null) {
            try {
                // It would be nicer to keep this connection open, but then we'd have to be closed.
                SshClient client = connectSsh();
                client.disconnect();
                canConnectSsh = true;
            } catch (Exception e) {
                LOGGER.warn("Error making SSH connection to instance", e);
                canConnectSsh = false;
            }
        }
        return canConnectSsh;
    }

    /**
     * Connect to the instance over SSH
     * 
     * @param instance
     *            instance to connect to
     * @return {@link SshClient} for connection
     */
    public SshClient connectSsh() {
        if (!sshConfig.isEnabled()) {
            throw new IllegalStateException();
        }

        ComputeService computeService = cloudClient.getJcloudsComputeService();

        String jcloudsId = cloudClient.getJcloudsId(instanceId);
        NodeMetadata node = getJcloudsNode(computeService, jcloudsId);

        LoginCredentials credentials = sshConfig.getCredentials();
        node = NodeMetadataBuilder.fromNodeMetadata(node).credentials(credentials).build();

        Utils utils = computeService.getContext().getUtils();
        SshClient ssh = utils.sshForNode().apply(node);

        ssh.connect();

        return ssh;
    }

    private NodeMetadata getJcloudsNode(ComputeService computeService, String jcloudsId) {
        // Work around a jclouds bug / documentation issue...
        // TODO: Figure out what's broken, and eliminate this function

        // This should work (?):
        // Set<NodeMetadata> nodes = computeService.listNodesByIds(Collections.singletonList(jcloudsId));

        Set<NodeMetadata> nodes = Sets.newHashSet();
        for (ComputeMetadata n : computeService.listNodes()) {
            if (jcloudsId.equals(n.getId())) {
                nodes.add((NodeMetadata) n);
            }
        }

        if (nodes.isEmpty()) {
            LOGGER.warn("Unable to find jclouds node: {}", jcloudsId);
            for (ComputeMetadata n : computeService.listNodes()) {
                LOGGER.info("Did find node: {}", n);
            }
            throw new IllegalStateException("Unable to find node using jclouds: " + jcloudsId);
        }
        NodeMetadata node = Iterables.getOnlyElement(nodes);
        return node;
    }

}

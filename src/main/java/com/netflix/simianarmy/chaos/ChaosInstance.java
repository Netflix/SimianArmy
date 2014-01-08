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

import org.jclouds.domain.LoginCredentials;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.CloudClient;

/**
 * Wrapper around an instance on which we are going to cause chaos.
 */
public class ChaosInstance {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ChaosInstance.class);

    private final CloudClient cloudClient;
    private final String instanceId;
    private final SshConfig sshConfig;

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
     * Gets the {@link SshConfig} used to SSH to the instance.
     *
     * @return the {@link SshConfig}
     */
    public SshConfig getSshConfig() {
        return sshConfig;
    }

    /**
     * Gets the {@link CloudClient} used to access the cloud.
     *
     * @return the {@link CloudClient}
     */
    public CloudClient getCloudClient() {
        return cloudClient;
    }

    /**
     * Returns the instance id to identify the instance to the cloud client.
     *
     * @return instance id
     */
    public String getInstanceId() {
        return instanceId;
    }

    /**
     * Memoize canConnectSsh function.
     */
    private Boolean canConnectSsh = null;

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
     * Connect to the instance over SSH.
     *
     * @return {@link SshClient} for connection
     */
    public SshClient connectSsh() {
        if (!sshConfig.isEnabled()) {
            throw new IllegalStateException();
        }

        LoginCredentials credentials = sshConfig.getCredentials();
        SshClient ssh = cloudClient.connectSsh(instanceId, credentials);

        return ssh;
    }
}

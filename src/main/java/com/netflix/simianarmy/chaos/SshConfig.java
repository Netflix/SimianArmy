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

import java.io.File;
import java.io.IOException;
import org.jclouds.domain.LoginCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Holds SSH connection info, used for script-based chaos types.
 */
public class SshConfig {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SshConfig.class);

    /**
     * The SSH credentials to log on to an instance.
     */
    private final LoginCredentials sshCredentials;

    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     */
    public SshConfig(MonkeyConfiguration config) {
        String sshUser = config.getStrOrElse("simianarmy.chaos.ssh.user", "root");
        String privateKey = null;

        String sshKeyPath = config.getStrOrElse("simianarmy.chaos.ssh.key", null);
        if (sshKeyPath != null) {
            sshKeyPath = sshKeyPath.trim();
            if (sshKeyPath.startsWith("~/")) {
                String home = System.getProperty("user.home");
                if (!Strings.isNullOrEmpty(home)) {
                    if (!home.endsWith("/")) {
                        home += "/";
                    }
                    sshKeyPath = home + sshKeyPath.substring(2);
                }
            }

            LOGGER.debug("Reading SSH key from {}", sshKeyPath);

            try {
                privateKey = Files.toString(new File(sshKeyPath), Charsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the specified SSH key: " + sshKeyPath, e);
            }
        }

        if (privateKey == null) {
        	   this.sshCredentials = LoginCredentials.builder().user(sshUser).build();
        } else {
            this.sshCredentials = LoginCredentials.builder().user(sshUser).privateKey(privateKey).build();
        }
    }

    /**
     * Get the configured SSH credentials.
     *
     * @return configured SSH credentials
     */
    public LoginCredentials getCredentials() {
        return sshCredentials;
    }

    /**
     * Check if ssh is configured.
     *
     * @return true if credentials are configured
     */
    public boolean isEnabled() {
        return sshCredentials != null;
    }
}

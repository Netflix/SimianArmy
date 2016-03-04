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

import java.io.IOException;
import java.net.URL;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Base class for chaos types that run a script over JClouds/SSH on the node.
 */
public abstract class ScriptChaosType extends ChaosType {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptChaosType.class);

    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @param key
     *            Key for the chaos money
     */
    public ScriptChaosType(MonkeyConfiguration config, String key) {
        super(config, key);
    }

    /**
     * We can apply the strategy iff we can SSH to the instance.
     */
    @Override
    public boolean canApply(ChaosInstance instance) {
        if (!instance.getSshConfig().isEnabled()) {
            LOGGER.info("Strategy disabled because SSH credentials not set");
            return false;
        }

        if (!instance.canConnectSsh(instance)) {
            LOGGER.warn("Strategy disabled because SSH credentials failed");
            return false;
        }

        return super.canApply(instance);
    }

    /**
     * Runs the script.
     */
    @Override
    public void apply(ChaosInstance instance) {
        LOGGER.info("Running script for {} on instance {}", getKey(), instance.getInstanceId());

        SshClient ssh = instance.connectSsh();

        String filename = getKey().toLowerCase() + ".sh";
        URL url = Resources.getResource(ScriptChaosType.class, "/scripts/" + filename);
        String script;
        try {
            script = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading script resource", e);
        }

        ssh.put("/tmp/" + filename, script);
        ExecResponse response = ssh.exec("/bin/bash /tmp/" + filename);
        if (response.getExitStatus() != 0) {
            LOGGER.warn("Got non-zero output from running script: {}", response);
        }
        ssh.disconnect();
    }
}

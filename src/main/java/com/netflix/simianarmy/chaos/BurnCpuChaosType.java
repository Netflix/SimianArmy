package com.netflix.simianarmy.chaos;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.Utils;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Executes a CPU intensive program on the node, using up all available CPU.
 *
 * This simulates either a noisy CPU neighbor on the box or just a general issue with the CPU.
 */
public class BurnCpuChaosType extends ChaosType {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BurnCpuChaosType.class);

    /**
     * The SSH credentials to log on to an instance.
     */
    private final LoginCredentials sshCredentials;

    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public BurnCpuChaosType(MonkeyConfiguration config) {
        super(config, "BurnCpu");

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
            try {
                privateKey = Files.toString(new File(sshKeyPath), Charsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read the specified SSH key: " + sshKeyPath, e);
            }
        }

        if (privateKey == null) {
            this.sshCredentials = null;
        } else {
            this.sshCredentials = LoginCredentials.builder().user(sshUser).privateKey(privateKey).build();
        }
    }

    /**
     * We can apply the strategy iff we can SSH to the instance.
     */
    @Override
    public boolean canApply(CloudClient cloudClient, String instanceId) {
        // TODO: Check that SSH connection works here?

        if (this.sshCredentials == null) {
            LOGGER.info("Strategy disabled because SSH credentials not set");
            return false;
        }

        return super.canApply(cloudClient, instanceId);
    }

    /**
     * Runs the cpu burn script.
     */
    @Override
    public void apply(CloudClient cloudClient, String instanceId) {
        ComputeService computeService = cloudClient.getJcloudsComputeService();

        String jcloudsId = cloudClient.getJcloudsId(instanceId);

        // Work around a jclouds bug / documentation issue...
        //Set<NodeMetadata> nodes = computeService.listNodesByIds(Collections.singletonList(jcloudsId));
        Set<NodeMetadata> nodes = Sets.newHashSet();
        for (ComputeMetadata n : computeService.listNodes()) {
            if (jcloudsId.equals(n.getId())) {
                nodes.add((NodeMetadata) n);
            }
        }

        if (nodes.isEmpty()) {
            LOGGER.warn("Unable to jclouds node: {}", jcloudsId);
            for (ComputeMetadata n : computeService.listNodes()) {
                LOGGER.info("Did find node: {}", n);
            }
            throw new IllegalStateException("Unable to find node using jclouds: " + jcloudsId);
        }
        NodeMetadata node = Iterables.getOnlyElement(nodes);

        node = NodeMetadataBuilder.fromNodeMetadata(node).credentials(sshCredentials).build();

        LOGGER.info("Burning CPU on instance {}", instanceId);

        Utils utils = computeService.getContext().getUtils();
        SshClient ssh = utils.sshForNode().apply(node);

        ssh.connect();

        URL url = Resources.getResource("/scripts/burncpu.sh");
        String script;
        try {
            script = Resources.toString(url, Charsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading script resource", e);
        }

        ssh.put("/tmp/burncpu.sh", script);
        ExecResponse response = ssh.exec("/bin/bash /tmp/burncpu.sh");
        if (response.getExitStatus() != 0) {
            LOGGER.warn("Got non-zero output from running script: {}", response);
        }
        ssh.disconnect();
    }
}

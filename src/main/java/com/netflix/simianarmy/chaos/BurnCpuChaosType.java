package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Executes a CPU intensive program on the node, using up all available CPU.
 *
 * This simulates either a noisy CPU neighbor on the box or just a general issue with the CPU.
 */
public class BurnCpuChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public BurnCpuChaosType(MonkeyConfiguration config) {
        super(config, "BurnCpu");
    }
}

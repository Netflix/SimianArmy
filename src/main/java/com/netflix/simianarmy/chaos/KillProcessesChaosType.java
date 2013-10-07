package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Kills processes on the node.
 *
 * This simulates the process crashing (for any reason).
 */
public class KillProcessesChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public KillProcessesChaosType(MonkeyConfiguration config) {
        super(config, "KillProcesses");
    }
}

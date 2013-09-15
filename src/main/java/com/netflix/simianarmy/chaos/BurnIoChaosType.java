package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Executes a disk I/O intensive program on the node, reducing I/O capacity.
 *
 * This simulates either a noisy neighbor on the box or just a general issue with the disk.
 */
public class BurnIoChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public BurnIoChaosType(MonkeyConfiguration config) {
        super(config, "BurnIO");
    }
}

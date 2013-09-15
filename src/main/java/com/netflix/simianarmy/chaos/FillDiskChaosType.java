package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Creates a huge file on the root device so that the disk fills up.
 */
public class FillDiskChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public FillDiskChaosType(MonkeyConfiguration config) {
        super(config, "FillDisk");
    }
}

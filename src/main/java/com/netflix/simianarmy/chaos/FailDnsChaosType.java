package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Blocks TCP & UDP port 53, so DNS resolution fails.
 */
public class FailDnsChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public FailDnsChaosType(MonkeyConfiguration config) {
        super(config, "FailDns");
    }
}

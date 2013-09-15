package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Introduces network packet corruption using traffic-shaping.
 */
public class NetworkCorruptionChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public NetworkCorruptionChaosType(MonkeyConfiguration config) {
        super(config, "NetworkCorruption");
    }
}

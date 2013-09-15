package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Introduces network packet loss using traffic-shaping.
 */
public class NetworkLossChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public NetworkLossChaosType(MonkeyConfiguration config) {
        super(config, "NetworkLoss");
    }
}

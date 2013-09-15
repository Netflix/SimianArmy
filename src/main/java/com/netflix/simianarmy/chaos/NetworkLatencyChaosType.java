package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Introduces network latency using traffic-shaping.
 */
public class NetworkLatencyChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public NetworkLatencyChaosType(MonkeyConfiguration config) {
        super(config, "NetworkLatency");
    }
}

package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Adds entries to /etc/hosts so that AWS API endpoints are unreachable.
 */
public class FailApiChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public FailApiChaosType(MonkeyConfiguration config) {
        super(config, "FailApi");
    }
}

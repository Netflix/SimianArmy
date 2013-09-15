package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Adds entries to /etc/hosts so that S3 API endpoints are unreachable.
 */
public class FailS3ChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public FailS3ChaosType(MonkeyConfiguration config) {
        super(config, "FailS3");
    }
}

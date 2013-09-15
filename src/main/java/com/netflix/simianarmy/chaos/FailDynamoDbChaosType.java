package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Adds entries to /etc/hosts so that DynamoDB API endpoints are unreachable.
 */
public class FailDynamoDbChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public FailDynamoDbChaosType(MonkeyConfiguration config) {
        super(config, "FailDynamoDb");
    }
}

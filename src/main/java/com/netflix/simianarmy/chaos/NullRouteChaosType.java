package com.netflix.simianarmy.chaos;

import java.io.IOException;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Null routes the network, taking a node going offline.
 *
 * Currently we offline 10.x.x.x (the AWS private network range).
 *
 * I think the machine will still be publicly accessible, but won't be able to communicate with any other nodes on
 * the EC2 network.
 */
public class NullRouteChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     * @throws IOException
     */
    public NullRouteChaosType(MonkeyConfiguration config) {
        super(config, "NullRoute");
    }
}

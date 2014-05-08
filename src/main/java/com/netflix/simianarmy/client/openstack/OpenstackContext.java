package com.netflix.simianarmy.client.openstack;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.BasicChaosMonkeyContext;

/**
 * OpenstackContext.
 */
public class OpenstackContext extends BasicChaosMonkeyContext {
    private OpenstackClient client;

    /**
     * Init context.
     */
    public OpenstackContext() {
        client = null;
        createClient();
    }

    /** {@inheritDoc} */
    @Override
    protected void createClient() {
        final MonkeyConfiguration config = configuration();
        final OpenstackServiceConnection conn = new OpenstackServiceConnection(
                config);
        client = new OpenstackClient(conn);
        setCloudClient(client);
        setChaosCrawler(new OpenstackChaosCrawler(client));
    }
}

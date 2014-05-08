package com.netflix.simianarmy.chaos;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.jclouds.ssh.SshClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.client.openstack.OpenstackClient;

/**
 * Generic class to block access to an Openstack API endpoint by adding entries
 * in the IPTables.
 */
public class FailOpenstackEndpointChaosType extends ChaosType {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(BlockAllNetworkTrafficChaosType.class);
    private final String endpointType;

    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     */
    public FailOpenstackEndpointChaosType(final MonkeyConfiguration config,
            final String endpointType) {
        super(config, "Fail" + endpointType);
        this.endpointType = endpointType;
    }

    /**
     * We can apply the strategy iff the endpoint type exists.
     */
    @Override
    public boolean canApply(final ChaosInstance instance) {
        final OpenstackClient cloudClient = (OpenstackClient) instance
                .getCloudClient();

        return cloudClient.getEndpoints().containsKey(endpointType);
    }

    /**
     * Null-routes the openstack endpoint.
     */
    @Override
    public void apply(final ChaosInstance instance) {
        final OpenstackClient cloudClient = (OpenstackClient) instance
                .getCloudClient();
        instance.getInstanceId();
        final SshClient ssh = instance.connectSsh();
        for (final String endpoint : cloudClient.getEndpoints().get(
                endpointType)) {
            try {
                final URL endpointURL = new URL(endpoint);
                final InetAddress address = InetAddress.getByName(endpointURL
                        .getHost());
                ssh.exec("sudo iptables -A OUTPUT -d "
                        + address.getHostAddress() + " -p tcp -m tcp --dport "
                        + endpointURL.getPort() + " -j DROP");
                ssh.exec("sudo iptables -A OUTPUT -d "
                        + address.getHostAddress() + " -p udp -m udp --dport "
                        + endpointURL.getPort() + " -j DROP");

            } catch (final MalformedURLException e) {
                e.printStackTrace();
            } catch (final UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }
}


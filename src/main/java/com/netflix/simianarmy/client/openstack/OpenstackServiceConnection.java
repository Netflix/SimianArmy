package com.netflix.simianarmy.client.openstack;

import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Contains meta-data for the OpenstackClient.
 */
public class OpenstackServiceConnection {
    private String userName = null;
    private String tenantName = null;
    private String password = null;
    private String provider = null;
    private String url = null;
    private String zone = null;

    /**
     * Instantiate the OpenstackServiceConnection using the inputs from the
     * configuration files.
     *
     * @param config
     *            Data from the configuration files
     */
    public OpenstackServiceConnection(final MonkeyConfiguration config) {
        userName = config.getStr("simianarmy.client.openstack.userName");
        url = config.getStr("simianarmy.client.openstack.url");
        provider = config.getStrOrElse("simianarmy.client.openstack.provider",
                "openstack-nova");
        tenantName = config.getStr("simianarmy.client.openstack.tenantName");
        password = config.getStr("simianarmy.client.openstack.password");
        zone = config.getStrOrElse("simianarmy.client.openstack.zone",
                "regionOne");
    }

    /**
     * Get the zone for the deployment.
     *
     * @return zone Zone name
     */
    public String getZone() {
        return zone;
    }

    /**
     * Set the zone for the deployment.
     *
     * @param zone
     *            Zone name
     */
    public void setZone(final String zone) {
        this.zone = zone;
    }

    /**
     * Get the username to be used to authenticate with Keystone.
     *
     * @return userName User name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Set the username to be used to authenticate with Keystone.
     *
     * @param userName
     *            User name
     */
    public void setUserName(final String userName) {
        this.userName = userName;
    }

    /**
     * Get the password to be used to authenticate with Keystone.
     *
     * @return password Password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password to be used to authenticate with Keystone.
     *
     * @param password
     *            Password
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * Get the tenant name which the monkeys will target.
     *
     * @return tenantName Tenant Name
     */
    public String getTenantName() {
        return tenantName;
    }

    /**
     * Set the tenant name which the monkeys will target.
     *
     * @param tenantName
     *            Tenant Name
     */
    public void setTenantName(final String tenantName) {
        this.tenantName = tenantName;
    }

    /**
     * Get the Nova API endpoint.
     *
     * @return url Nova Endpoint
     */
    public String getUrl() {
        return url;
    }

    /**
     * Set the Nova API endpoint.
     *
     * @param url
     *            Nova Endpoint
     */
    public void setUrl(final String url) {
        this.url = url;
    }

    /**
     * Get the provider string.
     *
     * @return provider
     */
    public String getProvider() {
        return provider;
    }

    /**
     * Set the provider string.
     *
     * @param provider
     */
    public void setProvider(final String provider) {
        this.provider = provider;
    }
}

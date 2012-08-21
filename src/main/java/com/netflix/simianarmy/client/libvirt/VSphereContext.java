package com.netflix.simianarmy.client.libvirt;

import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.BasicContext;

public class VSphereContext extends BasicContext {
    @Override
    protected void createClient(BasicConfiguration config) {
        this.awsClient = new VSphereClient(config);
        setCloudClient(this.awsClient);
    }
}

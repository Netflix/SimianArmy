package com.netflix.simianarmy.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.BasicContext;

public class VSphereContext extends BasicContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(VSphereContext.class);

    @Override
    protected void createClient(BasicConfiguration config) {
		LOGGER.info("IS24DatacenterContext.createspecificClient()");
		this.awsClient = new VSphereClient(config);
        setCloudClient(this.awsClient);
    }
}

package com.netflix.simianarmy.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.basic.BasicContext;
import com.netflix.simianarmy.basic.IS24Client;

public class VMWareContext extends BasicContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(VMWareContext.class);

    @Override
    protected void createspecificClient(String account, String secret, String region) {
		LOGGER.info("IS24DatacenterContext.createspecificClient()");
		this.awsClient = new IS24Client(account, secret, region);
    }
}

package com.netflix.simianarmy.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IS24DatacenterContext extends BasicContext {
    private static final Logger LOGGER = LoggerFactory.getLogger(IS24DatacenterContext.class);

    @Override
    protected void createspecificClient(String account, String secret, String region) {
		LOGGER.info("IS24DatacenterContext.createspecificClient()");
		this.awsClient = new IS24Client(account, secret, region);
    }
}

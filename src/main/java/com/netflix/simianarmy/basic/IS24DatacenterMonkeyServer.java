package com.netflix.simianarmy.basic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("serial")
public class IS24DatacenterMonkeyServer extends BasicMonkeyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(IS24DatacenterMonkeyServer.class);

	@SuppressWarnings("rawtypes")
	@Override
	protected Class getContextClass() {
		LOGGER.info("########## IS24DatacenterMonkeyServer.getContextClass()");
		return IS24DatacenterContext.class;
	}
}

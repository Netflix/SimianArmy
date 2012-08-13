/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.simianarmy.basic;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.chaos.ChaosMonkey;

@SuppressWarnings("serial")
public class BasicMonkeyServer extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicMonkeyServer.class);

    private static final MonkeyRunner RUNNER = MonkeyRunner.getInstance();

    @SuppressWarnings("unchecked")
	public void addMonkeysToRun() {
        RUNNER.replaceMonkey(getMonkeyClass(), this.clientContextClass);
    }

	@SuppressWarnings("rawtypes")
    public Class clientContextClass = com.netflix.simianarmy.basic.BasicContext.class; 
    
	@SuppressWarnings("rawtypes")
	protected Class getMonkeyClass() {
		return BasicChaosMonkey.class;
	}

    @Override
    public void init() throws ServletException {
        super.init();
        configureClient();
        addMonkeysToRun();
        RUNNER.start();
    }

    /**
     * Loads the client that is configured
     * 
     * @throws ServletException if the configured client cannot be loaded properly 
     */
    private void configureClient() throws ServletException {
    	Properties clientConfig = loadClientConfigProperties();

    	loadClientContextClass(clientConfig);

	}

	private void loadClientContextClass(Properties clientConfig) throws ServletException {
		String clientContextClassKey = "client.context.class";
    	ClassLoader classLoader = BasicMonkeyServer.class.getClassLoader();
        try {
			String clientContextClassName = clientConfig.getProperty(clientContextClassKey);
			this.clientContextClass = classLoader.loadClass(clientContextClassName );
            LOGGER.info("as "+clientContextClassKey+" loaded "+clientContextClass.getCanonicalName());
        } catch (ClassNotFoundException e) {
            throw new ServletException("Could not load " +clientContextClassKey,e);
        }
	}

    /**
     * Load the client config properties file
     *  
     * @return Properties The contents of the client config dile
     * @throws ServletException if file cannot be read
     */
	private Properties loadClientConfigProperties() throws ServletException {
		String clientConfigFileName = "/client.properties"; //TODO IK read as servlet attribute //(String) getServletContext().getInitParameter("CLIENT_PROPERTIES");
    	LOGGER.info("using client properties "+clientConfigFileName);
    	
    	InputStream input = null;
    	Properties p = new Properties();
    	try {
    		try {
	    		input = BasicMonkeyServer.class.getResourceAsStream(clientConfigFileName);
				p.load(input);
				return p;
    		}finally {
    			if (input != null) input.close();
    		}
    	} catch (IOException e) {
			throw new ServletException("Could not load "+clientConfigFileName,e);
    	}
	}

    @SuppressWarnings("unchecked")
	@Override
    public void destroy() {
        RUNNER.stop();
        RUNNER.removeMonkey(getMonkeyClass());
        super.destroy();
    }
}

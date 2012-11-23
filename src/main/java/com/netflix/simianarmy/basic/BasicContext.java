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

import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.aws.SimpleDBRecorder;
import com.netflix.simianarmy.basic.chaos.BasicChaosInstanceSelector;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.client.aws.chaos.ASGChaosCrawler;

/**
 * The Class BasicContext. This provide the basic context needed for the monkeys to run. It will configure the monkeys
 * based on a simianarmy.properties file. The properties file can be override with
 * -Dsimianarmy.properties=/path/to/my.properties
 */
public class BasicContext extends BasicContextShell {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicContext.class);

    /** The client configuration properties. */
    private Properties props = new Properties();

    /** The Constant MONKEY_THREADS. */
    private static final int MONKEY_THREADS = 1;

    /** The client used to interact with the target machines. */
    private AWSClient awsClient;

    public AWSClient getAwsClient() {
        return awsClient;
    }

    public void setAwsClient(AWSClient awsClient) {
        this.awsClient = awsClient;
    }

    /** loads all relevant configuration files to configure the client. */
    protected void addClientConfigurationProperties() {
        loadClientConfigurationIntoProperties("simianarmy.properties");
        loadClientConfigurationIntoProperties("client.properties");
    }

    /** loads the given config ontop of the config read by previous calls. */
    protected void loadClientConfigurationIntoProperties(String propertyFileName) {
        String propFile = System.getProperty(propertyFileName, "/" + propertyFileName);
        try {
            InputStream is = BasicContext.class.getResourceAsStream(propFile);
            try {
                props.load(is);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            String msg = "Unable to load properties file " + propFile + " set System property \"" + propertyFileName
                    + "\" to valid file";
            LOGGER.error(msg);
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Instantiates a new basic context.
     */
    public BasicContext() {
        BasicConfiguration config = loadClientConfig();

        setCalendar(new BasicCalendar(config));

        createClient(config);

        createScheduler(config);

        setChaosCrawler(new ASGChaosCrawler(this.awsClient));
        setChaosInstanceSelector(new BasicChaosInstanceSelector());

        createRecorder(config);
    }

    private BasicConfiguration loadClientConfig() {
        addClientConfigurationProperties();
        BasicConfiguration config = new BasicConfiguration(props);
        setConfiguration(config);
        return config;
    }

    private void createScheduler(BasicConfiguration config) {
        int freq = (int) config.getNumOrElse("simianarmy.scheduler.frequency", 1);
        TimeUnit freqUnit = TimeUnit.valueOf(config.getStrOrElse("simianarmy.scheduler.frequencyUnit", "HOURS"));
        int threads = (int) config.getNumOrElse("simianarmy.scheduler.threads", MONKEY_THREADS);
        setScheduler(new BasicScheduler(freq, freqUnit, threads));
    }

    private void createRecorder(BasicConfiguration config) {
        String account = config.getStr("simianarmy.aws.accountKey");
        String secret = config.getStr("simianarmy.aws.secretKey");
        String region = config.getStrOrElse("simianarmy.aws.region", "us-east-1");
        String domain = config.getStrOrElse("simianarmy.sdb.domain", "SIMIAN_ARMY");
        setRecorder(new SimpleDBRecorder(account, secret, region, domain));
    }

    /**
     * Create the specific client. Override to provide your own client.
     */
    protected void createClient(BasicConfiguration config) {
        String account = config.getStr("simianarmy.aws.accountKey");
        String secret = config.getStr("simianarmy.aws.secretKey");
        String region = config.getStrOrElse("simianarmy.aws.region", "us-east-1");
        this.awsClient = new AWSClient(account, secret, region);

        setCloudClient(this.awsClient);
    }

}

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

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.io.InputStream;

import com.netflix.simianarmy.aws.AWSClient;
import com.netflix.simianarmy.aws.SimpleDBRecorder;

import com.netflix.simianarmy.basic.chaos.BasicChaosCrawler;
import com.netflix.simianarmy.basic.chaos.BasicChaosInstanceSelector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class BasicContext. This provde the basix context needed for the monkeys to run. It will confiugre the monkeys
 * based on a simianarmy.properties file. The properties file can be override with
 * -Dsimianarmy.properties=/path/to/my.properties
 */
public class BasicContext extends BasicContextShell {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicContext.class);

    /** The Constant PROPS. */
    private static final Properties PROPS;

    /** The Constant MONKEY_THREADS. */
    private static final int MONKEY_THREADS = 4;
    static {
        String propFile = System.getProperty("simianarmy.properties", "/simianarmy.properties");
        PROPS = new Properties();
        try {
            InputStream is = BasicContext.class.getResourceAsStream(propFile);
            try {
                PROPS.load(is);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            LOGGER.error("Unable to load properties file " + propFile
                    + " set System property \"simianarmy.properties\" to valid file");
        }
    }

    /**
     * Instantiates a new basic context.
     */
    public BasicContext() {
        BasicConfiguration config = new BasicConfiguration(PROPS);
        setConfiguration(config);
        setCalendar(new BasicCalendar(config));
        String account = config.getStr("simianarmy.aws.accountKey");
        String secret = config.getStr("simianarmy.aws.secretKey");
        String region = config.getStrOrElse("simianarmy.aws.region", "us-east-1");
        AWSClient client = new AWSClient(account, secret, region);
        setCloudClient(client);
        int freq = (int) config.getNumOrElse("simianarmy.scheduler.frequency", 1);
        TimeUnit freqUnit = TimeUnit.valueOf(config.getStrOrElse("simianarmy.scheduler.frequencyUnit", "HOURS"));
        int threads = (int) config.getNumOrElse("simianarmy.scheduler.threads", MONKEY_THREADS);
        setScheduler(new BasicScheduler(freq, freqUnit, threads));
        setChaosCrawler(new BasicChaosCrawler(client));
        setChaosInstanceSelector(new BasicChaosInstanceSelector());
        String domain = config.getStrOrElse("simianarmy.sdb.domain", "SIMIAN_ARMY");
        setRecorder(new SimpleDBRecorder(account, secret, region, domain));
    }
}

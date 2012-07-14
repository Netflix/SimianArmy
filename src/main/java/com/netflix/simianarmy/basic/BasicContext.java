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
import java.io.InputStream;

import com.netflix.simianarmy.chaos.ChaosMonkey;

import com.netflix.simianarmy.MonkeyScheduler;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.chaos.ChaosInstanceSelector;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.aws.AWSClient;
import com.netflix.simianarmy.aws.SimpleDBRecorder;

import com.netflix.simianarmy.basic.chaos.BasicChaosCrawler;
import com.netflix.simianarmy.basic.chaos.BasicChaosInstanceSelector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicContext implements ChaosMonkey.Context {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicContext.class);
    private MonkeyScheduler scheduler;
    private MonkeyCalendar calendar;
    private MonkeyConfiguration config;
    private AWSClient client;
    private ChaosCrawler crawler;
    private ChaosInstanceSelector selector;
    private MonkeyRecorder recorder;
    private static final int MONKEY_THREADS = 4;

    private static final Properties PROPS;
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

    public BasicContext() {
        calendar = new BasicCalendar();
        config = new BasicConfiguration(PROPS);
        String account = config.getStr("accountKey");
        String secret = config.getStr("secretKey");
        String region = config.getStrOrElse("region", "us-east-1");
        client = new AWSClient(account, secret, region);
        scheduler = new BasicScheduler((int) config.getNumOrElse("monkey.threads", MONKEY_THREADS));
        crawler = new BasicChaosCrawler(client);
        selector = new BasicChaosInstanceSelector();
        String domain = config.getStrOrElse("domain", "SIMIAN_ARMY");
        recorder = new SimpleDBRecorder(account, secret, region, domain);
    }

    @Override
    public MonkeyScheduler scheduler() {
        return scheduler;
    }

    @Override
    public MonkeyCalendar calendar() {
        return calendar;
    }

    @Override
    public MonkeyConfiguration configuration() {
        return config;
    }

    @Override
    public CloudClient cloudClient() {
        return client;
    }

    @Override
    public ChaosCrawler chaosCrawler() {
        return crawler;
    }

    @Override
    public ChaosInstanceSelector chaosInstanceSelector() {
        return selector;
    }

    @Override
    public MonkeyRecorder recorder() {
        return recorder;
    }

}

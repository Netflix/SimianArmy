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

import com.netflix.simianarmy.chaos.ChaosMonkey;

import com.netflix.simianarmy.MonkeyScheduler;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.chaos.ChaosInstanceSelector;
import com.netflix.simianarmy.aws.AWSClient;

public class BasicContext implements ChaosMonkey.Context {
    private MonkeyScheduler scheduler;
    private MonkeyCalendar calendar;
    private MonkeyConfiguration config;
    private AWSClient client;
    private ChaosCrawler crawler;
    private ChaosInstanceSelector selector;

    public BasicContext(Properties props) {
        scheduler = new BasicScheduler();
        calendar = new BasicCalendar();
        config = new BasicConfiguration(props);
        client = new AWSClient(config.getStr("accountKey"), config.getStr("secretKey"), config.getStrOrElse(
                "region", "us-east-1"));
        crawler = new BasicChaosCrawler(client);
        selector = new ChaosInstanceSelector();
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

}

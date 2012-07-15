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

import com.netflix.simianarmy.chaos.ChaosMonkey;

import com.netflix.simianarmy.MonkeyScheduler;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.chaos.ChaosInstanceSelector;
import com.netflix.simianarmy.MonkeyRecorder;

public class BasicContextShell implements ChaosMonkey.Context {
    private MonkeyScheduler scheduler;
    private MonkeyCalendar calendar;
    private MonkeyConfiguration config;
    private CloudClient client;
    private ChaosCrawler crawler;
    private ChaosInstanceSelector selector;
    private MonkeyRecorder recorder;

    @Override
    public MonkeyScheduler scheduler() {
        return scheduler;
    }

    protected void setScheduler(MonkeyScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public MonkeyCalendar calendar() {
        return calendar;
    }

    protected void setCalendar(MonkeyCalendar calendar) {
        this.calendar = calendar;
    }

    @Override
    public MonkeyConfiguration configuration() {
        return config;
    }

    protected void setConfiguration(MonkeyConfiguration configuration) {
        this.config = configuration;
    }

    @Override
    public CloudClient cloudClient() {
        return client;
    }

    protected void setCloudClient(CloudClient cloudClient) {
        this.client = cloudClient;
    }

    @Override
    public ChaosCrawler chaosCrawler() {
        return crawler;
    }

    protected void setChaosCrawler(ChaosCrawler chaosCrawler) {
        this.crawler = chaosCrawler;
    }

    @Override
    public ChaosInstanceSelector chaosInstanceSelector() {
        return selector;
    }

    protected void setChaosInstanceSelector(ChaosInstanceSelector chaosInstanceSelector) {
        this.selector = chaosInstanceSelector;
    }

    @Override
    public MonkeyRecorder recorder() {
        return recorder;
    }

    protected void setRecorder(MonkeyRecorder recorder) {
        this.recorder = recorder;
    }
}

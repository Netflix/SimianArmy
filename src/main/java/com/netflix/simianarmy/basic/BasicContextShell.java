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

import java.util.LinkedList;

import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.MonkeyScheduler;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.chaos.ChaosInstanceSelector;
import com.netflix.simianarmy.chaos.ChaosMonkey;

/**
 * The Class BasicContextShell.
 */
public class BasicContextShell implements ChaosMonkey.Context {

    /** The scheduler. */
    private MonkeyScheduler scheduler;

    /** The calendar. */
    private MonkeyCalendar calendar;

    /** The config. */
    private MonkeyConfiguration config;

    /** The client. */
    private CloudClient client;

    /** The crawler. */
    private ChaosCrawler crawler;

    /** The selector. */
    private ChaosInstanceSelector selector;

    /** The recorder. */
    private MonkeyRecorder recorder;

    private LinkedList<Event> eventReport;

    /** protected constructor as the Shell is meant to be subclassed. */
    protected BasicContextShell() {
        eventReport = new LinkedList<Event>();
    }
    
    @Override
    public void eventReport(Event evt) {
        this.eventReport.add(evt);
    }
    
    @Override
    public void resetEventReport() {
        eventReport.clear();
    }

    @Override
    public String getEventReport() {
        StringBuilder report = new StringBuilder();
        
        for (Event event : this.eventReport) {
            report
            .append(event.eventType())
            .append(" ")
            .append(event.id())
            .append(" (")
            .append(event.field("groupType"))
            .append(", ")
            .append(event.field("groupName"))
            .append(")\n");
        }
        return report.toString();
    }
    
    /** {@inheritDoc} */
    @Override
    public MonkeyScheduler scheduler() {
        return scheduler;
    }

    /**
     * Sets the scheduler.
     *
     * @param scheduler
     *            the new scheduler
     */
    protected void setScheduler(MonkeyScheduler scheduler) {
        this.scheduler = scheduler;
    }

    /** {@inheritDoc} */
    @Override
    public MonkeyCalendar calendar() {
        return calendar;
    }

    /**
     * Sets the calendar.
     *
     * @param calendar
     *            the new calendar
     */
    protected void setCalendar(MonkeyCalendar calendar) {
        this.calendar = calendar;
    }

    /** {@inheritDoc} */
    @Override
    public MonkeyConfiguration configuration() {
        return config;
    }

    /**
     * Sets the configuration.
     *
     * @param configuration
     *            the new configuration
     */
    protected void setConfiguration(MonkeyConfiguration configuration) {
        this.config = configuration;
    }

    /** {@inheritDoc} */
    @Override
    public CloudClient cloudClient() {
        return client;
    }

    /**
     * Sets the cloud client.
     *
     * @param cloudClient
     *            the new cloud client
     */
    protected void setCloudClient(CloudClient cloudClient) {
        this.client = cloudClient;
    }

    /** {@inheritDoc} */
    @Override
    public ChaosCrawler chaosCrawler() {
        return crawler;
    }

    /**
     * Sets the chaos crawler.
     *
     * @param chaosCrawler
     *            the new chaos crawler
     */
    protected void setChaosCrawler(ChaosCrawler chaosCrawler) {
        this.crawler = chaosCrawler;
    }

    /** {@inheritDoc} */
    @Override
    public ChaosInstanceSelector chaosInstanceSelector() {
        return selector;
    }

    /**
     * Sets the chaos instance selector.
     *
     * @param chaosInstanceSelector
     *            the new chaos instance selector
     */
    protected void setChaosInstanceSelector(ChaosInstanceSelector chaosInstanceSelector) {
        this.selector = chaosInstanceSelector;
    }

    /** {@inheritDoc} */
    @Override
    public MonkeyRecorder recorder() {
        return recorder;
    }

    /**
     * Sets the recorder.
     *
     * @param recorder
     *            the new recorder
     */
    protected void setRecorder(MonkeyRecorder recorder) {
        this.recorder = recorder;
    }
}

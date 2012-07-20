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

import com.netflix.simianarmy.MonkeyScheduler;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The Class BasicScheduler.
 */
public class BasicScheduler implements MonkeyScheduler {

    /** The futures. */
    private HashMap<String, ScheduledFuture<?>> futures = new HashMap<String, ScheduledFuture<?>>();

    /** The scheduler. */
    private final ScheduledExecutorService scheduler;

    /**
     * Instantiates a new basic scheduler.
     */
    public BasicScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Instantiates a new basic scheduler.
     *
     * @param concurrent
     *            the concurrent number of threads
     */
    public BasicScheduler(int concurrent) {
        scheduler = Executors.newScheduledThreadPool(concurrent);
    }

    /** {@inheritDoc} */
    @Override
    public int frequency() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public TimeUnit frequencyUnit() {
        return TimeUnit.HOURS;
    }

    /** {@inheritDoc} */
    @Override
    public void start(String name, Runnable command) {
        futures.put(name, scheduler.scheduleWithFixedDelay(command, 0, frequency(), frequencyUnit()));
    }

    /** {@inheritDoc} */
    @Override
    public void stop(String name) {
        if (futures.containsKey(name)) {
            futures.remove(name).cancel(true);
        }
    }
}

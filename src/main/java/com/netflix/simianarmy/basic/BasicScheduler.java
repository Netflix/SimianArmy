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

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.MonkeyScheduler;

/**
 * The Class BasicScheduler.
 */
public class BasicScheduler implements MonkeyScheduler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicScheduler.class);

    /** The futures. */
    private HashMap<String, ScheduledFuture<?>> futures = new HashMap<String, ScheduledFuture<?>>();

    /** The scheduler. */
    private final ScheduledExecutorService scheduler;

    /** the frequency. */
    private int frequency = 1;

    /** the frequencyUnit. */
    private TimeUnit frequencyUnit = TimeUnit.HOURS;

    /**
     * Instantiates a new basic scheduler.
     */
    public BasicScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Instantiates a new basic scheduler.
     *
     * @param freq
     *            the frequency to run on
     * @param freqUnit
     *            the unit for the freq argument
     * @param concurrent
     *            the concurrent number of threads
     */
    public BasicScheduler(int freq, TimeUnit freqUnit, int concurrent) {
        frequency = freq;
        frequencyUnit = freqUnit;
        scheduler = Executors.newScheduledThreadPool(concurrent);
    }

    /** {@inheritDoc} */
    @Override
    public int frequency() {
        return frequency;
    }

    /** {@inheritDoc} */
    @Override
    public TimeUnit frequencyUnit() {
        return frequencyUnit;
    }

    /** {@inheritDoc} */
    @Override
    public void start(Monkey monkey, Runnable command) {
        long cycle = TimeUnit.MILLISECONDS.convert(frequency(), frequencyUnit());

        // go back 1 cycle to see if we have any events
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MILLISECOND, (int) (-1 * cycle));

        Date then = cal.getTime();
        List<Event> events = monkey.context().recorder()
                .findEvents(monkey.type(), Collections.<String, String>emptyMap(), then);
        if (events.isEmpty()) {
            // no events so just run now
            futures.put(monkey.type().name(),
                    scheduler.scheduleWithFixedDelay(command, 0, frequency(), frequencyUnit()));
        } else {
            // we have events, so set the start time to the time left in what would have been the last cycle
            Date eventTime = events.get(0).eventTime();
            Date now = new Date();
            long init = cycle - (now.getTime() - eventTime.getTime());
            LOGGER.info("Detected previous events within cycle, setting " + monkey.type().name() + " start to "
                    + new Date(now.getTime() + init));
            futures.put(monkey.type().name(),
                    scheduler.scheduleWithFixedDelay(command, init, cycle, TimeUnit.MILLISECONDS));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void stop(Monkey monkey) {
        if (futures.containsKey(monkey.type().name())) {
            futures.remove(monkey.type().name()).cancel(true);
        }
    }
}

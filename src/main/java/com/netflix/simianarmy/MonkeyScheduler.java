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
package com.netflix.simianarmy;

import java.util.concurrent.TimeUnit;

/**
 * The Interface MonkeyScheduler.
 */
public interface MonkeyScheduler {

    /**
     * Frequency. How often the monkey should run, works in conjunction with frequencyUnit(). If frequency is 2 and
     * frequencyUnit is TimeUnit.HOUR then the monkey will run once ever 2 hours.
     *
     * @return the frequency interval
     */
    int frequency();

    /**
     * Frequency unit. This is the time unit that corresponds with frequency().
     *
     * @return time unit
     */
    TimeUnit frequencyUnit();

    /**
     * Start the scheduler to cause the monkey run at a specified interval.
     *
     * @param monkey
     *            the monkey being scheduled
     * @param run
     *            the Runnable to start, generally calls doMonkeyBusiness
     */
    void start(Monkey monkey, Runnable run);

    /**
     * Stop the scheduler for a given monkey. After this the monkey will no longer run on the fixed schedule.
     *
     * @param monkey
     *            the monkey being scheduled
     */
    void stop(Monkey monkey);
}

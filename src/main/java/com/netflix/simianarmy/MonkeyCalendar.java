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

import java.util.Calendar;
import java.util.Date;

/**
 * The Interface MonkeyCalendar used to tell if a monkey should be running or now. We only want monkeys to run during
 * business hours, so that engineers will be on-hand if something goes wrong.
 */
public interface MonkeyCalendar {

    /**
     * Checks if is monkey time.
     *
     * @param monkey
     *            the monkey
     * @return true, if is monkey time
     */
    boolean isMonkeyTime(Monkey monkey);

    /**
     * Open hour. This is the "open" hour for then the monkey should start working.
     *
     * @return the int
     */
    int openHour();

    /**
     * Close hour. This is the "close" hour for when the monkey should stop working.
     *
     * @return the int
     */
    int closeHour();

    /**
     * Get the current time using whatever timezone is used for monkey date calculations.
     *
     * @return the calendar
     */
    Calendar now();

    /** Gets the next business day from the start date after n business days.
     *
     * @param date the start date
     * @param n the number of business days from now
     * @return the business day after n business days
     */
    Date getBusinessDay(Date date, int n);
}

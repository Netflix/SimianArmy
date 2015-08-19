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
// CHECKSTYLE IGNORE Javadoc
// CHECKSTYLE IGNORE MagicNumberCheck
package com.netflix.simianarmy.aws.janitor.rule;

import java.util.Calendar;
import java.util.Date;

import org.joda.time.DateTime;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyCalendar;

/**
 * The class is an implementation of MonkeyCalendar that can always run and
 * considers calendar days only when calculating the termination date.
 *
 */
public class TestMonkeyCalendar implements MonkeyCalendar {
    @Override
    public boolean isMonkeyTime(Monkey monkey) {
        return true;
    }

    @Override
    public int openHour() {
        return 0;
    }

    @Override
    public int closeHour() {
        return 24;
    }

    @Override
    public Calendar now() {
        return Calendar.getInstance();
    }

    @Override
    public Date getBusinessDay(Date date, int n) {
        DateTime target = new DateTime(date.getTime()).plusDays(n);
        return new Date(target.getMillis());
    }
}

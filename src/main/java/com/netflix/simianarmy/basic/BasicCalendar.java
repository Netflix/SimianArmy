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
import java.util.Set;
import java.util.TreeSet;
import java.util.TimeZone;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.Monkey;

// CHECKSTYLE IGNORE MagicNumberCheck
public class BasicCalendar implements MonkeyCalendar {
    private final int openHour;
    private final int closeHour;
    private final TimeZone tz;
    private final Set<Integer> holidays = new TreeSet<Integer>();

    private MonkeyConfiguration cfg;

    public BasicCalendar(MonkeyConfiguration cfg) {
        this.cfg = cfg;
        openHour = 9;
        closeHour = 15;
        tz = TimeZone.getTimeZone("America/Los_Angeles");
    }

    public BasicCalendar(int open, int close, TimeZone timezone) {
        openHour = open;
        closeHour = close;
        tz = timezone;
    }

    @Override
    public int openHour() {
        return openHour;
    }

    @Override
    public int closeHour() {
        return closeHour;
    }

    @Override
    public Calendar now() {
        return Calendar.getInstance(tz);
    }

    @Override
    public boolean isMonkeyTime(Monkey monkey) {
        if (cfg.getStrOrElse("simianarmy.isMonkeyTime", null) != null) {
            return cfg.getBool("simianarmy.isMonkeyTime");
        }

        Calendar now = now();
        int dow = now.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
            return false;
        }

        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (hour < openHour || hour > closeHour) {
            return false;
        }

        if (isHoliday(now)) {
            return false;
        }

        return true;
    }

    protected boolean isHoliday(Calendar now) {
        if (!holidays.contains(now.get(Calendar.YEAR))) {
            loadHolidays(now.get(Calendar.YEAR));
        }
        return holidays.contains(now.get(Calendar.DAY_OF_YEAR));
    }

    protected void loadHolidays(int year) {
        holidays.clear();
        // these aren't all strictly holidays, but days when engineers will likely
        // not be in the office to respond to rampaging monkeys

        // new years, or closest work day
        holidays.add(workDayInYear(Calendar.JANUARY, 1));

        // 3rd monday == MLK Day
        holidays.add(dayOfYear(Calendar.JANUARY, Calendar.MONDAY, 3));

        // 3rd monday == Presidents Day
        holidays.add(dayOfYear(Calendar.FEBRUARY, Calendar.MONDAY, 3));

        // last monday == Memorial Day
        holidays.add(dayOfYear(Calendar.MAY, Calendar.MONDAY, -1));

        // 4th of July, or closest work day
        holidays.add(workDayInYear(Calendar.JULY, 4));

        // first monday == Labor Day
        holidays.add(dayOfYear(Calendar.SEPTEMBER, Calendar.MONDAY, 1));

        // second monday == Columbus Day
        holidays.add(dayOfYear(Calendar.OCTOBER, Calendar.MONDAY, 2));

        // veterans day, Nov 11th, or closest work day
        holidays.add(workDayInYear(Calendar.NOVEMBER, 11));

        // 4th thursday == Thanksgiving
        holidays.add(dayOfYear(Calendar.NOVEMBER, Calendar.THURSDAY, 4));

        // 4th friday == "black friday", monkey goes shopping!
        holidays.add(dayOfYear(Calendar.NOVEMBER, Calendar.FRIDAY, 4));

        // christmas eve
        holidays.add(dayOfYear(Calendar.DECEMBER, 24));
        // christmas day
        holidays.add(dayOfYear(Calendar.DECEMBER, 25));
        // day after christmas
        holidays.add(dayOfYear(Calendar.DECEMBER, 26));

        // new years eve
        holidays.add(dayOfYear(Calendar.DECEMBER, 31));

        // mark the holiday set with the year, so on Jan 1 it will automatically
        // recalculate the holidays for next year
        holidays.add(year);
    }

    private int dayOfYear(int month, int day) {
        Calendar holiday = now();
        holiday.set(Calendar.MONTH, month);
        holiday.set(Calendar.DAY_OF_MONTH, day);
        return holiday.get(Calendar.DAY_OF_YEAR);
    }

    private int dayOfYear(int month, int dayOfWeek, int weekInMonth) {
        Calendar holiday = now();
        holiday.set(Calendar.MONTH, month);
        holiday.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        holiday.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekInMonth);
        return holiday.get(Calendar.DAY_OF_YEAR);
    }

    private int workDayInYear(int month, int day) {
        Calendar holiday = now();
        holiday.set(Calendar.MONTH, month);
        holiday.set(Calendar.DAY_OF_MONTH, day);
        int doy = holiday.get(Calendar.DAY_OF_YEAR);
        int dow = holiday.get(Calendar.DAY_OF_WEEK);

        if (dow == Calendar.SATURDAY) {
            return doy - 1; // FRIDAY
        }

        if (dow == Calendar.SUNDAY) {
            return doy + 1; // MONDAY
        }

        return doy;
    }

}

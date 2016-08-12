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
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;

// CHECKSTYLE IGNORE MagicNumberCheck
/**
 * The Class BasicCalendar.
 */
public class BasicCalendar implements MonkeyCalendar {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicCalendar.class);

    /** The open hour. */
    private final int openHour;

    /** The close hour. */
    private final int closeHour;

    /** The tz. */
    private final TimeZone tz;

    /** The holidays. */
    protected final Set<Integer> holidays = new TreeSet<Integer>();

    /** The cfg. */
    private MonkeyConfiguration cfg;

    /**
     * Instantiates a new basic calendar.
     *
     * @param cfg
     *            the monkey configuration
     */
    public BasicCalendar(MonkeyConfiguration cfg) {
        this.cfg = cfg;
        openHour = (int) cfg.getNumOrElse("simianarmy.calendar.openHour", 9);
        closeHour = (int) cfg.getNumOrElse("simianarmy.calendar.closeHour", 15);
        tz = TimeZone.getTimeZone(cfg.getStrOrElse("simianarmy.calendar.timezone", "America/Los_Angeles"));
    }

    /**
     * Instantiates a new basic calendar.
     *
     * @param open
     *            the open hour
     * @param close
     *            the close hour
     * @param timezone
     *            the timezone
     */
    public BasicCalendar(int open, int close, TimeZone timezone) {
        openHour = open;
        closeHour = close;
        tz = timezone;
    }

    /**
     * Instantiates a new basic calendar.
     *
     * @param open
     *            the open hour
     * @param close
     *            the close hour
     * @param timezone
     *            the timezone
     */
    public BasicCalendar(MonkeyConfiguration cfg, int open, int close, TimeZone timezone) {
        this.cfg = cfg;
        openHour = open;
        closeHour = close;
        tz = timezone;
    }

    /** {@inheritDoc} */
    @Override
    public int openHour() {
        return openHour;
    }

    /** {@inheritDoc} */
    @Override
    public int closeHour() {
        return closeHour;
    }

    /** {@inheritDoc} */
    @Override
    public Calendar now() {
        return Calendar.getInstance(tz);
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMonkeyTime(Monkey monkey) {
        if (cfg != null && cfg.getStr("simianarmy.calendar.isMonkeyTime") != null) {
        	boolean monkeyTime = cfg.getBool("simianarmy.calendar.isMonkeyTime");
        	if (monkeyTime) {
        		LOGGER.debug("isMonkeyTime: Found property 'simianarmy.calendar.isMonkeyTime': " + monkeyTime + ". Time for monkey.");
        		return monkeyTime;
        	} else {
        		LOGGER.debug("isMonkeyTime: Found property 'simianarmy.calendar.isMonkeyTime': " + monkeyTime + ". Continuing regular calendar check for monkey time.");
        	}
        }

        Calendar now = now();
        int dow = now.get(Calendar.DAY_OF_WEEK);
        if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
        	LOGGER.debug("isMonkeyTime: Happy Weekend! Not time for monkey.");
            return false;
        }

        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (hour < openHour || hour > closeHour) {
        	LOGGER.debug("isMonkeyTime: Not inside open hours. Not time for monkey.");
            return false;
        }

        if (isHoliday(now)) {
        	LOGGER.debug("isMonkeyTime: Happy Holiday! Not time for monkey.");
            return false;
        }

    	LOGGER.debug("isMonkeyTime: Time for monkey.");
        return true;
    }

    /**
     * Checks if is holiday.
     *
     * @param now
     *            the current time
     * @return true, if is holiday
     */
    protected boolean isHoliday(Calendar now) {
        if (!holidays.contains(now.get(Calendar.YEAR))) {
            loadHolidays(now.get(Calendar.YEAR));
        }
        return holidays.contains(now.get(Calendar.DAY_OF_YEAR));
    }

    /**
     * Load holidays.
     *
     * @param year
     *            the year
     */
    protected void loadHolidays(int year) {
        holidays.clear();
        // these aren't all strictly holidays, but days when engineers will likely
        // not be in the office to respond to rampaging monkeys

        // new years, or closest work day
        holidays.add(workDayInYear(year, Calendar.JANUARY, 1));

        // 3rd monday == MLK Day
        holidays.add(dayOfYear(year, Calendar.JANUARY, Calendar.MONDAY, 3));

        // 3rd monday == Presidents Day
        holidays.add(dayOfYear(year, Calendar.FEBRUARY, Calendar.MONDAY, 3));

        // last monday == Memorial Day
        holidays.add(dayOfYear(year, Calendar.MAY, Calendar.MONDAY, -1));

        // 4th of July, or closest work day
        holidays.add(workDayInYear(year, Calendar.JULY, 4));

        // first monday == Labor Day
        holidays.add(dayOfYear(year, Calendar.SEPTEMBER, Calendar.MONDAY, 1));

        // second monday == Columbus Day
        holidays.add(dayOfYear(year, Calendar.OCTOBER, Calendar.MONDAY, 2));

        // veterans day, Nov 11th, or closest work day
        holidays.add(workDayInYear(year, Calendar.NOVEMBER, 11));

        // 4th thursday == Thanksgiving
        holidays.add(dayOfYear(year, Calendar.NOVEMBER, Calendar.THURSDAY, 4));

        // 4th friday == "black friday", monkey goes shopping!
        holidays.add(dayOfYear(year, Calendar.NOVEMBER, Calendar.FRIDAY, 4));

        // christmas eve
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 24));
        // christmas day
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 25));
        // day after christmas
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 26));

        // new years eve
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 31));

        // mark the holiday set with the year, so on Jan 1 it will automatically
        // recalculate the holidays for next year
        holidays.add(year);
    }

    /**
     * Day of year.
     *
     * @param year
     *            the year
     * @param month
     *            the month
     * @param day
     *            the day
     * @return the day of the year
     */
    protected int dayOfYear(int year, int month, int day) {
        Calendar holiday = now();
        holiday.set(Calendar.YEAR, year);
        holiday.set(Calendar.MONTH, month);
        holiday.set(Calendar.DAY_OF_MONTH, day);
        return holiday.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Day of year.
     *
     * @param year
     *            the year
     * @param month
     *            the month
     * @param dayOfWeek
     *            the day of week
     * @param weekInMonth
     *            the week in month
     * @return the day of the year
     */
    protected int dayOfYear(int year, int month, int dayOfWeek, int weekInMonth) {
        Calendar holiday = now();
        holiday.set(Calendar.YEAR, year);
        holiday.set(Calendar.MONTH, month);
        holiday.set(Calendar.DAY_OF_WEEK, dayOfWeek);
        holiday.set(Calendar.DAY_OF_WEEK_IN_MONTH, weekInMonth);
        return holiday.get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Work day in year.
     *
     * @param year
     *            the year
     * @param month
     *            the month
     * @param day
     *            the day
     * @return the day of the year adjusted to the closest workday
     */
    protected int workDayInYear(int year, int month, int day) {
        Calendar holiday = now();
        holiday.set(Calendar.YEAR, year);
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

    @Override
    public Date getBusinessDay(Date date, int n) {
        Validate.isTrue(n >= 0);
        Calendar calendar = now();
        calendar.setTime(date);
        while (isHoliday(calendar) || isWeekend(calendar) || n-- > 0) {
            calendar.add(Calendar.DATE, 1);
        }
        return calendar.getTime();
    }

    private boolean isWeekend(Calendar calendar) {
        int dow = calendar.get(Calendar.DAY_OF_WEEK);
        return dow == Calendar.SATURDAY || dow == Calendar.SUNDAY;
    }

}

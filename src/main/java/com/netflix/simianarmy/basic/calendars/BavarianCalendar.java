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
package com.netflix.simianarmy.basic.calendars;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.BasicCalendar;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;

// CHECKSTYLE IGNORE MagicNumberCheck
/**
 * The Class BavarianCalendar.
 */
public class BavarianCalendar extends BasicCalendar
{
    /**
     * Instantiates a new basic calendar.
     *
     * @param cfg  the monkey configuration
     */
    public BavarianCalendar(MonkeyConfiguration cfg)
    {
        super(cfg);
    }

    /** {@inheritDoc} */
    @Override
    protected void loadHolidays(int year) {
        holidays.clear();

        // these aren't all strictly holidays, but days when engineers will likely
        // not be in the office to respond to rampaging monkeys

        // first of all, we need easter sunday doy,
        // because ome other holidays calculated from it
        int easter = westernEasterDayOfYear(year);

        // new year
        holidays.addAll(getHolidayWithBridgeDays(year, dayOfYear(year, Calendar.JANUARY, 1)));

        // epiphanie
        holidays.addAll(getHolidayWithBridgeDays(year, dayOfYear(year, Calendar.JANUARY, 6)));

        // good friday, always friday, don't need to check if it's bridge day
        holidays.add(easter - 2);

        // easter monday, always monday, don't need to check if it's bridge day
        holidays.add(easter + 1);

        // labor day
        holidays.addAll(getHolidayWithBridgeDays(year, dayOfYear(year, Calendar.MAY, 1)));

        // ascension day
        holidays.addAll(getHolidayWithBridgeDays(year, easter + 39));

        // whit monday, always monday, don't need to check if it's bridge day
        holidays.add(easter + 50);

        // corpus christi
        holidays.add(westernEasterDayOfYear(year) + 60);

        // assumption day
        holidays.addAll(getHolidayWithBridgeDays(year, dayOfYear(year, Calendar.AUGUST, 15)));

        // german unity day
        holidays.addAll(getHolidayWithBridgeDays(year, dayOfYear(year, Calendar.OCTOBER, 3)));

        // all saints
        holidays.addAll(getHolidayWithBridgeDays(year, dayOfYear(year, Calendar.NOVEMBER, 1)));

        // monkey goes on christmas vacations between christmas and new year!
        holidays.addAll(getHolidayWithBridgeDays(year, dayOfYear(year, Calendar.DECEMBER, 24)));
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 25));
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 26));
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 27));
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 28));
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 29));
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 30));
        holidays.add(dayOfYear(year, Calendar.DECEMBER, 31));

        // mark the holiday set with the year, so on Jan 1 it will automatically
        // recalculate the holidays for next year
        holidays.add(year);
    }

    /**
     * Returns collection of holidays, including Monday or Friday
     * if given holiday is Thuesday or Thursday.
     *
     * The behaviour to take Monday as day off if official holiday is Thuesday
     * and to take Friday as day off if official holiday is Thursday
     * is specific to [at least] Germany.
     * We call it, literally, "bridge day".
     *
     * @param dayOfYear  holiday day of year
     */
    private Collection<Integer> getHolidayWithBridgeDays(int year, int dayOfYear) {
        Calendar holiday = now();
        holiday.set(Calendar.YEAR, year);
        holiday.set(Calendar.DAY_OF_YEAR, dayOfYear);
        int dow = holiday.get(Calendar.DAY_OF_WEEK);
        int mon = holiday.get(Calendar.MONTH);
        int dom = holiday.get(Calendar.DAY_OF_MONTH);

        // We don't want to include Monday if Thuesday is January 1.
        if (dow == Calendar.TUESDAY && dayOfYear != 1)
            return Arrays.asList(dayOfYear, dayOfYear - 1);

        // We don't want to include Friday if Thursday is December 31.
        if (dow == Calendar.THURSDAY && (mon != Calendar.DECEMBER || dom != 31))
            return Arrays.asList(dayOfYear, dayOfYear + 1);

        return Arrays.asList(dayOfYear);
    }

    /**
     * Western easter sunday in year.
     *
     * @param year
     *            the year
     * @return the day of the year of western easter sunday
     */
    protected int westernEasterDayOfYear(int year) {
        int a = year % 19,
                b = year / 100,
                c = year % 100,
                d = b / 4,
                e = b % 4,
                g = (8 * b + 13) / 25,
                h = (19 * a + b - d - g + 15) % 30,
                j = c / 4,
                k = c % 4,
                m = (a + 11 * h) / 319,
                r = (2 * e + 2 * j - k - h + m + 32) % 7;
        int oneBasedMonth = (h - m + r + 90) / 25;
        int dayOfYear = (h - m + r + oneBasedMonth + 19) % 32;
        return dayOfYear(year, oneBasedMonth - 1, dayOfYear);
    }

}

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
package com.netflix.simianarmy.basic.calendar;

import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.calendars.BavarianCalendar;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.Properties;

// CHECKSTYLE IGNORE MagicNumberCheck

public class TestBavarianCalendar extends BavarianCalendar {
    private static final Properties PROPS = new Properties();
    private static final BasicConfiguration CFG = new BasicConfiguration(PROPS);

    public TestBavarianCalendar() {
        super(CFG);
    }

    private Calendar now = super.now();

    @Override
    public Calendar now() {
        return (Calendar) now.clone();
    }

    private void setNow(Calendar now) {
        this.now = now;
    }

    @DataProvider
    public Object[][] easterDataProvider() {
        return new Object[][] {
                {1996, Calendar.APRIL, 7},
                {1997, Calendar.MARCH, 30},
                {1998, Calendar.APRIL, 12},
                {1999, Calendar.APRIL, 4},
                {2000, Calendar.APRIL, 23},
                {2001, Calendar.APRIL, 15},
                {2002, Calendar.MARCH, 31},
                {2003, Calendar.APRIL, 20},
                {2004, Calendar.APRIL, 11},
                {2005, Calendar.MARCH, 27},
                {2006, Calendar.APRIL, 16},
                {2007, Calendar.APRIL, 8},
                {2008, Calendar.MARCH, 23},
                {2009, Calendar.APRIL, 12},
                {2010, Calendar.APRIL, 4},
                {2011, Calendar.APRIL, 24},
                {2012, Calendar.APRIL, 8},
                {2013, Calendar.MARCH, 31},
                {2014, Calendar.APRIL, 20},
                {2015, Calendar.APRIL, 5},
                {2016, Calendar.MARCH, 27},
                {2017, Calendar.APRIL, 16},
                {2018, Calendar.APRIL, 1},
                {2019, Calendar.APRIL, 21},
                {2020, Calendar.APRIL, 12},
                {2021, Calendar.APRIL, 4},
                {2022, Calendar.APRIL, 17},
                {2023, Calendar.APRIL, 9},
                {2024, Calendar.MARCH, 31},
                {2025, Calendar.APRIL, 20},
                {2026, Calendar.APRIL, 5},
                {2027, Calendar.MARCH, 28},
                {2028, Calendar.APRIL, 16},
                {2029, Calendar.APRIL, 1},
                {2030, Calendar.APRIL, 21},
                {2031, Calendar.APRIL, 13},
                {2032, Calendar.MARCH, 28},
                {2033, Calendar.APRIL, 17},
                {2034, Calendar.APRIL, 9},
                {2035, Calendar.MARCH, 25},
                {2036, Calendar.APRIL, 13},
        };
    }

    @Test(dataProvider = "easterDataProvider")
    public void testEaster(int year, int month, int dayOfMonth) {
        Assert.assertEquals(dayOfYear(year, month, dayOfMonth), westernEasterDayOfYear(year));
    }

    @DataProvider
    public Object[][] holidayDataProvider() {
        return new Object[][] {
                {2016, Calendar.JANUARY, 1},   // new year
                {2016, Calendar.JANUARY, 6},   // epiphanie
                {2016, Calendar.MARCH, 25},    // good friday
                {2016, Calendar.MARCH, 28},    // easter monday
                {2016, Calendar.MAY, 1},       // labor day
                {2016, Calendar.MAY, 5},       // ascension day
                {2016, Calendar.MAY, 6},       // friday after ascension day
                {2016, Calendar.MAY, 16},      // whit monday
                {2016, Calendar.MAY, 26},      // corpus christi
                {2016, Calendar.AUGUST, 15},   // assumption day
                {2016, Calendar.OCTOBER, 3},   // german unity day
                {2016, Calendar.DECEMBER, 24}, // christmas holidays
                {2016, Calendar.DECEMBER, 25},
                {2016, Calendar.DECEMBER, 26},
                {2016, Calendar.DECEMBER, 27},
                {2016, Calendar.DECEMBER, 28},
                {2016, Calendar.DECEMBER, 29},
                {2016, Calendar.DECEMBER, 30},
                {2016, Calendar.DECEMBER, 31},
                // now, "bridge days"
                {2015, Calendar.JANUARY, 2},   // friday after new year
                {2015, Calendar.JANUARY, 5},   // monday before epiphanie
                {2011, Calendar.JANUARY, 7},   // friday after epiphanie
                {2012, Calendar.APRIL, 30},    // monday before labor day
                {2014, Calendar.MAY, 2},       // friday after labor day
                {2006, Calendar.AUGUST, 14},   // monday before assumption day
                {2013, Calendar.AUGUST, 16},   // friday after assumption day
                {2006, Calendar.OCTOBER, 2},   // monday before german unity day
                {2013, Calendar.OCTOBER, 4},   // friday after german unity day
                {2011, Calendar.OCTOBER, 31},  // monday before all saints
                {2012, Calendar.NOVEMBER, 2},  // friday after all saints
                {2013, Calendar.DECEMBER, 23}  // monday before christas eve
        };
    }

    @Test(dataProvider = "holidayDataProvider")
    public void testHolidays(int year, int month, int dayOfMonth) {
        Calendar test = Calendar.getInstance();
        test.set(Calendar.YEAR, year);
        test.set(Calendar.MONTH, month);
        test.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        test.set(Calendar.HOUR_OF_DAY, 10);
        setNow(test);

        Assert.assertTrue(isHoliday(test), test.getTime().toString() + " is a holiday?");
    }

}

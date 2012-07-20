// CHECKSTYLE IGNORE Javadoc
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

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.TestMonkey;

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.DataProvider;

import java.util.TimeZone;
import java.util.Calendar;
import java.util.Properties;

// CHECKSTYLE IGNORE MagicNumberCheck

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBasicCalendar extends BasicCalendar {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestBasicCalendar.class);

    private static final Properties PROPS = new Properties();
    private static final BasicConfiguration CFG = new BasicConfiguration(PROPS);

    public TestBasicCalendar() {
        super(CFG);
    }

    @Test
    public void testConstructors() {
        BasicCalendar cal = new BasicCalendar(CFG);
        Assert.assertEquals(cal.openHour(), 9);
        Assert.assertEquals(cal.closeHour(), 15);
        Assert.assertEquals(cal.now().getTimeZone(), TimeZone.getTimeZone("America/Los_Angeles"));

        cal = new BasicCalendar(11, 12, TimeZone.getTimeZone("Europe/Stockholm"));
        Assert.assertEquals(cal.openHour(), 11);
        Assert.assertEquals(cal.closeHour(), 12);
        Assert.assertEquals(cal.now().getTimeZone(), TimeZone.getTimeZone("Europe/Stockholm"));
    }

    private Calendar now = super.now();

    public Calendar now() {
        return (Calendar) now.clone();
    }

    private void setNow(Calendar now) {
        this.now = now;
    }

    @Test
    void testMonkeyTime() {
        Calendar test = Calendar.getInstance();
        Monkey monkey = new TestMonkey();

        // using leap day b/c it is not a holiday & not a weekend
        test.set(Calendar.YEAR, 2012);
        test.set(Calendar.MONTH, Calendar.FEBRUARY);
        test.set(Calendar.DAY_OF_MONTH, 29);
        test.set(Calendar.HOUR_OF_DAY, 8); // 8am leap day
        setNow(test);

        Assert.assertFalse(isMonkeyTime(monkey));

        test.set(Calendar.HOUR_OF_DAY, 10); // 10am leap day
        setNow(test);

        Assert.assertTrue(isMonkeyTime(monkey));

        test.set(Calendar.HOUR_OF_DAY, 17); // 5pm leap day
        setNow(test);

        Assert.assertFalse(isMonkeyTime(monkey));

        // set to the following Saturday so we can test we dont run on weekends
        // even though within "business hours"
        test.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        test.set(Calendar.HOUR_OF_DAY, 10);
        setNow(test);
        Assert.assertFalse(isMonkeyTime(monkey));

        // test config overrides
        PROPS.setProperty("simianarmy.calendar.isMonkeyTime", Boolean.toString(true));
        Assert.assertTrue(isMonkeyTime(monkey));

        PROPS.setProperty("simianarmy.calendar.isMonkeyTime", Boolean.toString(false));
        Assert.assertFalse(isMonkeyTime(monkey));
    }

    @DataProvider
    public Object[][] holidayDataProvider() {
        return new Object[][] {{Calendar.JANUARY, 2}, // New Year's Day
                {Calendar.JANUARY, 16}, // MLK day
                {Calendar.FEBRUARY, 20}, // Washington's Birthday
                {Calendar.MAY, 28}, // Memorial Day
                {Calendar.JULY, 4}, // Independence Day
                {Calendar.SEPTEMBER, 3}, // Labor Day
                {Calendar.OCTOBER, 8}, // Columbus Day
                {Calendar.NOVEMBER, 12}, // Veterans Day
                {Calendar.NOVEMBER, 22}, // Thanksgiving Day
                {Calendar.DECEMBER, 25} // Christmas Day
        };
    }

    @Test(dataProvider = "holidayDataProvider")
    void testHolidays(int month, int dayOfMonth) {
        Calendar test = Calendar.getInstance();
        test.set(Calendar.YEAR, 2012);
        test.set(Calendar.MONTH, month);
        test.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        test.set(Calendar.HOUR_OF_DAY, 10);
        setNow(test);

        Assert.assertTrue(isHoliday(test), test.getTime().toString() + " is a holiday?");
    }
}

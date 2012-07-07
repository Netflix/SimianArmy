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
import java.util.Calendar;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Date;

import com.netflix.simianarmy.basic.BasicRecorderEvent;

import org.testng.Assert;

public class TestMonkeyContext implements Monkey.Context {
    private Enum monkeyType;

    public TestMonkeyContext(Enum monkeyType) {
        this.monkeyType = monkeyType;
    }

    public MonkeyScheduler scheduler() {
        return new MonkeyScheduler() {
            public int frequency() {
                return 1;
            }

            public TimeUnit frequencyUnit() {
                return TimeUnit.HOURS;
            }

            public void start(String name, Runnable run) {
                Assert.assertEquals(name, monkeyType.name(), "starting monkey");
                run.run();
            }

            public void stop(String name) {
                Assert.assertEquals(name, monkeyType.name(), "stopping monkey");
            }
        };
    }

    public MonkeyCalendar calendar() {
        // CHECKSTYLE IGNORE MagicNumberCheck
        return new MonkeyCalendar() {
            public boolean isMonkeyTime(Monkey monkey) {
                return true;
            }

            public int openHour() {
                return 10;
            }

            public int closeHour() {
                return 11;
            }

            public Calendar now() {
                return Calendar.getInstance();
            }
        };
    }

    public CloudClient cloudClient() {
        return new CloudClient() {
            public void terminateInstance(String instanceId) {
            }
        };
    }

    public MonkeyRecorder recorder() {
        return new MonkeyRecorder() {
            public Event newEvent(Enum mkType, Enum eventType, String id) {
                return new BasicRecorderEvent(mkType, eventType, id);
            }

            public void recordEvent(Event evt) {
            }

            public List<Event> findEvents(Map<String, String> query, Date after) {
                return new LinkedList<Event>();
            }

            public List<Event> findEvents(Enum mkeyType, Map<String, String> query, Date after) {
                return new LinkedList<Event>();
            }

            public List<Event> findEvents(Enum mkeyType, Enum eventType, Map<String, String> query, Date after) {
                return new LinkedList<Event>();
            }
        };
    }
}

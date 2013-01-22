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
package com.netflix.simianarmy;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;

import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.BasicRecorderEvent;

public class TestMonkeyContext implements Monkey.Context {
    private final Enum monkeyType;
    private final LinkedList<Event> eventReport = new LinkedList<Event>();

    public TestMonkeyContext(Enum monkeyType) {
        this.monkeyType = monkeyType;
    }

    @Override
    public MonkeyConfiguration configuration() {
        return new BasicConfiguration(new Properties());
    }

    @Override
    public MonkeyScheduler scheduler() {
        return new MonkeyScheduler() {
            @Override
            public int frequency() {
                return 1;
            }

            @Override
            public TimeUnit frequencyUnit() {
                return TimeUnit.HOURS;
            }

            @Override
            public void start(Monkey monkey, Runnable run) {
                Assert.assertEquals(monkey.type().name(), monkeyType.name(), "starting monkey");
                run.run();
            }

            @Override
            public void stop(Monkey monkey) {
                Assert.assertEquals(monkey.type().name(), monkeyType.name(), "stopping monkey");
            }
        };
    }

    @Override
    public MonkeyCalendar calendar() {
        // CHECKSTYLE IGNORE MagicNumberCheck
        return new MonkeyCalendar() {
            @Override
            public boolean isMonkeyTime(Monkey monkey) {
                return true;
            }

            @Override
            public int openHour() {
                return 10;
            }

            @Override
            public int closeHour() {
                return 11;
            }

            @Override
            public Calendar now() {
                return Calendar.getInstance();
            }

            @Override
            public Date getBusinessDay(Date date, int n) {
                throw new RuntimeException("Not implemented.");
            }
        };
    }

    @Override
    public CloudClient cloudClient() {
        return new CloudClient() {
            @Override
            public void terminateInstance(String instanceId) {
            }

            @Override
            public void createTagsForResources(Map<String, String> keyValueMap, String... resourceIds) {
            }

            @Override
            public void deleteAutoScalingGroup(String asgName) {
            }

            @Override
            public void deleteVolume(String volumeId) {
            }

            @Override
            public void deleteSnapshot(String snapshotId) {
            }

            @Override
            public void deleteLaunchConfiguration(String launchConfigName) {
            }
        };
    }

    private final MonkeyRecorder recorder = new MonkeyRecorder() {
        private final List<Event> events = new LinkedList<Event>();

        @Override
        public Event newEvent(Enum mkType, Enum eventType, String region, String id) {
            return new BasicRecorderEvent(mkType, eventType, region, id);
        }

        @Override
        public void recordEvent(Event evt) {
            events.add(evt);
        }

        @Override
        public List<Event> findEvents(Map<String, String> query, Date after) {
            return events;
        }

        @Override
        public List<Event> findEvents(Enum mkeyType, Map<String, String> query, Date after) {
            // used from BasicScheduler
            return events;
        }

        @Override
        public List<Event> findEvents(Enum mkeyType, Enum eventType, Map<String, String> query, Date after) {
            // used from ChaosMonkey
            List<Event> evts = new LinkedList<Event>();
            for (Event evt : events) {
                if (query.get("groupName").equals(evt.field("groupName")) && evt.monkeyType() == mkeyType
                        && evt.eventType() == eventType && evt.eventTime().after(after)) {
                    evts.add(evt);
                }
            }
            return evts;
        }
    };

    @Override
    public MonkeyRecorder recorder() {
        return recorder;
    }

    @Override
    public void reportEvent(Event evt) {
        eventReport.add(evt);
    }

    @Override
    public void resetEventReport() {
        eventReport.clear();
    }

    @Override
    public String getEventReport() {
        StringBuilder report = new StringBuilder();
        for (Event event : eventReport) {
            report.append(event.eventType());
            report.append(" ");
            report.append(event.id());
        }
        return report.toString();
    }
}

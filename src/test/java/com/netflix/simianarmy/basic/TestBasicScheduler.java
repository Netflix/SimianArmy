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

import org.testng.annotations.Test;
import org.testng.Assert;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.TestMonkeyContext;

// CHECKSTYLE IGNORE MagicNumber
public class TestBasicScheduler extends BasicScheduler {

    @Test
    public void testConstructors() {
        BasicScheduler sched = new BasicScheduler();
        Assert.assertNotNull(sched);
        Assert.assertEquals(sched.frequency(), 1);
        Assert.assertEquals(sched.frequencyUnit(), TimeUnit.HOURS);
        Assert.assertNotNull(new BasicScheduler(12, TimeUnit.HOURS, 2));
    }

    private int frequency = 2;

    public int frequency() {
        return frequency;
    }

    private TimeUnit frequencyUnit = TimeUnit.SECONDS;

    public TimeUnit frequencyUnit() {
        return frequencyUnit;
    }

    private enum Enums {
        MONKEY, EVENT
    };

    @Test
    public void testRunner() throws InterruptedException {
        BasicScheduler sched = new TestBasicScheduler();
        Monkey mockMonkey = mock(Monkey.class);
        when(mockMonkey.context()).thenReturn(new TestMonkeyContext(Enums.MONKEY));
        when(mockMonkey.type()).thenReturn(Enums.MONKEY).thenReturn(Enums.MONKEY);

        final AtomicLong counter = new AtomicLong(0L);
        sched.start(mockMonkey, new Runnable() {
            public void run() {
                counter.incrementAndGet();
            }
        });
        Thread.sleep(1000);
        Assert.assertEquals(counter.get(), 1);
        Thread.sleep(2000);
        Assert.assertEquals(counter.get(), 2);
        sched.stop(mockMonkey);
        Thread.sleep(2000);
        Assert.assertEquals(counter.get(), 2);
    }

    @Test
    public void testDelayedStart() throws InterruptedException {
        TestBasicScheduler sched = new TestBasicScheduler();

        // set monkey to run hourly
        sched.frequency = 1;
        sched.frequencyUnit = TimeUnit.HOURS;

        TestMonkeyContext context = new TestMonkeyContext(Enums.MONKEY);
        Monkey mockMonkey = mock(Monkey.class);
        when(mockMonkey.context()).thenReturn(context).thenReturn(context);
        when(mockMonkey.type()).thenReturn(Enums.MONKEY).thenReturn(Enums.MONKEY);

        // first monkey has no previous events, so it runs immediately
        final AtomicLong counter = new AtomicLong(0L);
        sched.start(mockMonkey, new Runnable() {
            public void run() {
                counter.incrementAndGet();
            }
        });
        Thread.sleep(10);
        Assert.assertEquals(counter.get(), 1);
        sched.stop(mockMonkey);

        // create an event 5 min ago
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -5);
        BasicRecorderEvent evt = new BasicRecorderEvent(Enums.MONKEY, Enums.EVENT, "region", "test-id", cal.getTime()
                .getTime());
        context.recorder().recordEvent(evt);

        // this time when it runs it will not increment within 10ms since it should be scheduled for 55m from now.
        sched.start(mockMonkey, new Runnable() {
            public void run() {
                counter.incrementAndGet();
            }
        });
        Thread.sleep(10);

        // counter did not increment because start was delayed due to previous event
        Assert.assertEquals(counter.get(), 1);
        sched.stop(mockMonkey);
    }
}

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
package com.netflix.simianarmy.basic;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

import java.util.Calendar;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.util.concurrent.Callables;
import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyType;
import com.netflix.simianarmy.TestMonkeyContext;

// CHECKSTYLE IGNORE MagicNumber
public class TestBasicScheduler {

    @Test
    public void testConstructors() {
        BasicScheduler sched = new BasicScheduler();
        Assert.assertNotNull(sched);
        Assert.assertEquals(sched.frequency(), 1);
        Assert.assertEquals(sched.frequencyUnit(), TimeUnit.HOURS);
        BasicScheduler sched2 = new BasicScheduler(12, TimeUnit.MINUTES, 2);
        Assert.assertEquals(sched2.frequency(), 12);
        Assert.assertEquals(sched2.frequencyUnit(), TimeUnit.MINUTES);
    }

    private enum Enums implements MonkeyType {
        MONKEY
    };

    private enum EventEnums implements EventType {
        EVENT
    }

    @Test
    public void testRunner() throws InterruptedException {
        BasicScheduler sched = new BasicScheduler(200, TimeUnit.MILLISECONDS, 1);
        Monkey mockMonkey = mock(Monkey.class);
        when(mockMonkey.context()).thenReturn(new TestMonkeyContext(Enums.MONKEY));
        when(mockMonkey.type()).thenReturn(Enums.MONKEY).thenReturn(Enums.MONKEY);

        final AtomicLong counter = new AtomicLong(0L);
        sched.start(mockMonkey, new Runnable() {
            @Override
            public void run() {
                counter.incrementAndGet();
            }
        });
        Thread.sleep(100);
        Assert.assertEquals(counter.get(), 1);
        Thread.sleep(200);
        Assert.assertEquals(counter.get(), 2);
        sched.stop(mockMonkey);
        Thread.sleep(200);
        Assert.assertEquals(counter.get(), 2);
    }

    @Test
    public void testDelayedStart() throws Exception {
        BasicScheduler sched = new BasicScheduler(1, TimeUnit.HOURS, 1);

        TestMonkeyContext context = new TestMonkeyContext(Enums.MONKEY);
        Monkey mockMonkey = mock(Monkey.class);
        when(mockMonkey.context()).thenReturn(context).thenReturn(context);
        when(mockMonkey.type()).thenReturn(Enums.MONKEY).thenReturn(Enums.MONKEY);

        // first monkey has no previous events, so it runs practically immediately
        FutureTask<Void> task = new FutureTask<Void>(Callables.<Void>returning(null));
        sched.start(mockMonkey, task);
        // make sure that the task gets completed within 100ms
        task.get(100L, TimeUnit.MILLISECONDS);
        sched.stop(mockMonkey);

        // create an event 5 min ago
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, -5);
        BasicRecorderEvent evt = new BasicRecorderEvent(
            Enums.MONKEY, EventEnums.EVENT, "region", "test-id", cal.getTime().getTime());
        context.recorder().recordEvent(evt);

        // this time when it runs it will not run immediately since it should be scheduled for 55m from now.
        task = new FutureTask<Void>(Callables.<Void>returning(null));
        sched.start(mockMonkey, task);
        try {
            task.get(100, TimeUnit.MILLISECONDS);
            Assert.fail("The task shouldn't have been completed in 100ms");
        } catch (TimeoutException e) { // NOPMD - This is an expected exception
        }
        sched.stop(mockMonkey);
    }
}

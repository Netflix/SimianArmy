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

import java.util.concurrent.TimeUnit;

public class TestBasicScheduler extends BasicScheduler {

    @Test
    public void testConstructors() {
        BasicScheduler sched = new BasicScheduler();
        Assert.assertNotNull(sched);
        Assert.assertEquals(sched.frequency(), 1);
        Assert.assertEquals(sched.frequencyUnit(), TimeUnit.HOURS);
        Assert.assertNotNull(new BasicScheduler(2));
    }

    private static int counter = 0;

    public int frequency() {
        return 2;
    }

    public TimeUnit frequencyUnit() {
        return TimeUnit.SECONDS;
    }

    @Test
    public void testRunner() throws InterruptedException {
        // CHECKSTYLE IGNORE MagicNumberCheck
        start("testRunner", new Runnable() {
            public void run() {
                counter++;
            }
        });
        Thread.sleep(1000);
        Assert.assertEquals(counter, 1);
        Thread.sleep(2000);
        Assert.assertEquals(counter, 2);
        stop("testRunner");
        Thread.sleep(2000);
        Assert.assertEquals(counter, 2);
    }
}

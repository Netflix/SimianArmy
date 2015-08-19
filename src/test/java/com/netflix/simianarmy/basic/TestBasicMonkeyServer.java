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

import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.TestMonkey;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext;

@SuppressWarnings("serial")
public class TestBasicMonkeyServer extends BasicMonkeyServer {
    private static final MonkeyRunner RUNNER = MonkeyRunner.getInstance();

    private static boolean monkeyRan = false;

    public static class SillyMonkey extends TestMonkey {
        @Override
        public void doMonkeyBusiness() {
            monkeyRan = true;
        }
    }

    @Override
    public void addMonkeysToRun() {
        MonkeyRunner.getInstance().replaceMonkey(BasicChaosMonkey.class, TestChaosMonkeyContext.class);
        MonkeyRunner.getInstance().addMonkey(SillyMonkey.class);
    }

    @Test
    public void testServer() {
        BasicMonkeyServer server = new TestBasicMonkeyServer();

        try {
            server.init();
        } catch (Exception e) {
            Assert.fail("failed to init server", e);
        }

        // there is a race condition since the monkeys will run
        // in a different thread. On some systems we might
        // need to add a sleep
        Assert.assertTrue(monkeyRan, "silly monkey ran");

        try {
            server.destroy();
        } catch (Exception e) {
            Assert.fail("failed to destroy server", e);
        }
        Assert.assertEquals(RUNNER.getMonkeys().size(), 1);
        Assert.assertEquals(RUNNER.getMonkeys().get(0).getClass(), SillyMonkey.class);
    }
}

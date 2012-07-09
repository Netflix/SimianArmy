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

import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.TestMonkey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBasicMonkeyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestBasicMonkeyServer.class);

    private static boolean monkeyRan = false;

    public static class SillyMonkey extends TestMonkey {
        public void doMonkeyBusiness() {
            monkeyRan = true;
        }
    }

    @Test
    public void testServer() {
        MonkeyRunner.getInstance().addMonkey(SillyMonkey.class);
        BasicMonkeyServer server = new BasicMonkeyServer();
        boolean init = false;
        try {
            server.init();
            init = true;
        } catch (Exception e) {
            LOGGER.error("failed to init server:", e);
        }

        Assert.assertTrue(init, "init server");
        // there is a race condition since the monkeys will run
        // in a different thread. On some systems we might
        // need to add a sleep
        Assert.assertTrue(monkeyRan, "silly monkey ran");

        boolean destroy = false;
        try {
            server.destroy();
            destroy = true;
        } catch (Exception e) {
            LOGGER.error("failed to destroy server:", e);
        }
        Assert.assertTrue(destroy, "destroy server");
    }
}

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
package com.netflix.simianarmy;

import java.util.List;

import org.testng.annotations.Test;
import org.testng.Assert;

public class TestMonkeyRunner {
    private static boolean monkeyARan = false;

    private static class MonkeyA extends TestMonkey {
        public void doMonkeyBusiness() {
            monkeyARan = true;
        }
    }

    private static boolean monkeyBRan = false;

    private static class MonkeyB extends Monkey {
        public enum Type implements MonkeyType {
            B
        };

        public Type type() {
            return Type.B;
        }

        public interface Context extends Monkey.Context {
            boolean getTrue();
        }

        private Context ctx;

        public MonkeyB(Context ctx) {
            super(ctx);
            this.ctx = ctx;
        }

        public void doMonkeyBusiness() {
            monkeyBRan = ctx.getTrue();
        }
    }

    private static class MonkeyBContext extends TestMonkeyContext implements MonkeyB.Context {
        public MonkeyBContext() {
            super(MonkeyB.Type.B);
        }

        public boolean getTrue() {
            return true;
        }
    }

    @Test
    void testInstance() {
        Assert.assertEquals(MonkeyRunner.getInstance(), MonkeyRunner.INSTANCE);
    }

    @Test
    void testRunner() {

        MonkeyRunner runner = MonkeyRunner.getInstance();

        runner.addMonkey(MonkeyA.class);
        runner.replaceMonkey(MonkeyA.class);

        runner.addMonkey(MonkeyB.class, MonkeyBContext.class);
        runner.replaceMonkey(MonkeyB.class, MonkeyBContext.class);

        List<Monkey> monkeys = runner.getMonkeys();
        Assert.assertEquals(monkeys.size(), 2);
        Assert.assertEquals(monkeys.get(0).type().name(), "TEST");
        Assert.assertEquals(monkeys.get(1).type().name(), "B");

        Monkey a = runner.factory(MonkeyA.class);
        Assert.assertEquals(a.type().name(), "TEST");

        Monkey b = runner.factory(MonkeyB.class, MonkeyBContext.class);
        Assert.assertEquals(b.type().name(), "B");

        Assert.assertNull(runner.getContextClass(MonkeyA.class));
        Assert.assertEquals(runner.getContextClass(MonkeyB.class), MonkeyBContext.class);

        runner.start();

        Assert.assertEquals(monkeyARan, true, "monkeyA ran");
        Assert.assertEquals(monkeyBRan, true, "monkeyB ran");

        runner.stop();

        runner.removeMonkey(MonkeyA.class);
        Assert.assertEquals(monkeys.size(), 1);
        Assert.assertEquals(monkeys.get(0).type().name(), "B");

        runner.removeMonkey(MonkeyB.class);
        Assert.assertEquals(monkeys.size(), 0);
    }
}

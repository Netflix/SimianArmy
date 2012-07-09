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
        public enum Type {
            B
        };

        public Enum type() {
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
    }
}

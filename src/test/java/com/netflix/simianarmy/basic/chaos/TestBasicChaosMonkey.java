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
package com.netflix.simianarmy.basic.chaos;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext;

// CHECKSTYLE IGNORE MagicNumberCheck
public class TestBasicChaosMonkey {
    @Test
    public void testDisabled() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("disabled.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 0, "no groups selected on");
        Assert.assertEquals(terminated.size(), 0, "nothing terminated");
    }

    @Test
    public void testEnabledA() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("enabledA.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 2);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(0).name(), "name0");
        Assert.assertEquals(selectedOn.get(1).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(1).name(), "name1");
        Assert.assertEquals(terminated.size(), 0, "nothing terminated");
    }

    @Test
    public void testUnleashedEnabledA() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("unleashedEnabledA.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 2);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(0).name(), "name0");
        Assert.assertEquals(selectedOn.get(1).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(1).name(), "name1");
        Assert.assertEquals(terminated.size(), 2);
        Assert.assertEquals(terminated.get(0), "0:i-123456780");
        Assert.assertEquals(terminated.get(1), "1:i-123456781");
    }

    @Test
    public void testEnabledB() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("enabledB.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 2);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_B);
        Assert.assertEquals(selectedOn.get(0).name(), "name2");
        Assert.assertEquals(selectedOn.get(1).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_B);
        Assert.assertEquals(selectedOn.get(1).name(), "name3");
        Assert.assertEquals(terminated.size(), 0, "nothing terminated");
    }

    @Test
    public void testUnleashedEnabledB() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("unleashedEnabledB.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 2);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_B);
        Assert.assertEquals(selectedOn.get(0).name(), "name2");
        Assert.assertEquals(selectedOn.get(1).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_B);
        Assert.assertEquals(selectedOn.get(1).name(), "name3");
        Assert.assertEquals(terminated.size(), 2);
        Assert.assertEquals(terminated.get(0), "2:i-123456782");
        Assert.assertEquals(terminated.get(1), "3:i-123456783");
    }

    @Test
    public void testEnabledAwithout1() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("enabledAwithout1.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 1);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(0).name(), "name0");
        Assert.assertEquals(terminated.size(), 1);
        Assert.assertEquals(terminated.get(0), "0:i-123456780");
    }

    @Test
    public void testEnabledAwith0() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("enabledAwith0.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 1);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(0).name(), "name0");
        Assert.assertEquals(terminated.size(), 1);
        Assert.assertEquals(terminated.get(0), "0:i-123456780");
    }

    @Test
    public void testAll() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("all.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 4);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(0).name(), "name0");
        Assert.assertEquals(selectedOn.get(1).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(1).name(), "name1");
        Assert.assertEquals(selectedOn.get(2).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_B);
        Assert.assertEquals(selectedOn.get(2).name(), "name2");
        Assert.assertEquals(selectedOn.get(3).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_B);
        Assert.assertEquals(selectedOn.get(3).name(), "name3");
        Assert.assertEquals(terminated.size(), 4);
        Assert.assertEquals(terminated.get(0), "0:i-123456780");
        Assert.assertEquals(terminated.get(1), "1:i-123456781");
        Assert.assertEquals(terminated.get(2), "2:i-123456782");
        Assert.assertEquals(terminated.get(3), "3:i-123456783");
    }

    @Test
    public void testNoProbability() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("noProbability.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 4);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(0).name(), "name0");
        Assert.assertEquals(selectedOn.get(1).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(1).name(), "name1");
        Assert.assertEquals(selectedOn.get(2).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_B);
        Assert.assertEquals(selectedOn.get(2).name(), "name2");
        Assert.assertEquals(selectedOn.get(3).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_B);
        Assert.assertEquals(selectedOn.get(3).name(), "name3");
        Assert.assertEquals(terminated.size(), 0);
    }

    @Test
    public void testNoProbabilityByName() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("noProbabilityByName.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 4);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(0).name(), "name0");
        Assert.assertEquals(selectedOn.get(1).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(1).name(), "name1");
        Assert.assertEquals(selectedOn.get(2).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_B);
        Assert.assertEquals(selectedOn.get(2).name(), "name2");
        Assert.assertEquals(selectedOn.get(3).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_B);
        Assert.assertEquals(selectedOn.get(3).name(), "name3");
        Assert.assertEquals(terminated.size(), 0);
    }

    @Test
    public void testMaxTerminationCountPerDayAsZero() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("terminationPerDayAsZero.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 0);
        Assert.assertEquals(ctx.terminated().size(), 0);
    }

    @Test
    public void testMaxTerminationCountPerDayAsOne() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("terminationPerDayAsOne.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);

        // Run the chaos the second time will NOT trigger another termination
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);
    }

    @Test
    public void testMaxTerminationCountPerDayAsBiggerThanOne() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("terminationPerDayAsBiggerThanOne.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);

        // Run the chaos the second time will trigger another termination
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 2);
        Assert.assertEquals(ctx.terminated().size(), 2);
    }

    @Test
    public void testMaxTerminationCountPerDayAsSmallerThanOne() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("terminationPerDayAsSmallerThanOne.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);

        // Run the chaos the second time will NOT trigger another termination
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);
    }

    @Test
    public void testMaxTerminationCountPerDayAsNegative() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("terminationPerDayAsNegative.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 0);
        Assert.assertEquals(ctx.terminated().size(), 0);
    }

    @Test
    public void testMaxTerminationCountPerDayAsVerySmall() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("terminationPerDayAsVerySmall.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 0);
        Assert.assertEquals(ctx.terminated().size(), 0);
    }

}

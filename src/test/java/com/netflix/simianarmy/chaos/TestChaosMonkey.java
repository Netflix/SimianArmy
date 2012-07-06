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
package com.netflix.simianarmy.chaos;

import java.util.List;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

import org.testng.annotations.Test;
import org.testng.Assert;

// CHECKSTYLE IGNORE MagicNumberCheck
public class TestChaosMonkey {
    @Test
    public void testDisabled() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(ChaosMonkey.class, "disabled.properties");
        ChaosMonkey chaos = new ChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(selectedOn.size(), 0, "no groups selected on");
        Assert.assertEquals(terminated.size(), 0, "nothing terminated");
    }

    @Test
    public void testEnabledA() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(ChaosMonkey.class, "enabledA.properties");
        ChaosMonkey chaos = new ChaosMonkey(ctx);
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
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(ChaosMonkey.class, "unleashedEnabledA.properties");
        ChaosMonkey chaos = new ChaosMonkey(ctx);
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
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(ChaosMonkey.class, "enabledB.properties");
        ChaosMonkey chaos = new ChaosMonkey(ctx);
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
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(ChaosMonkey.class, "unleashedEnabledB.properties");
        ChaosMonkey chaos = new ChaosMonkey(ctx);
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
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(ChaosMonkey.class, "enabledAwithout1.properties");
        ChaosMonkey chaos = new ChaosMonkey(ctx);
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
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(ChaosMonkey.class, "enabledAwith0.properties");
        ChaosMonkey chaos = new ChaosMonkey(ctx);
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
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(ChaosMonkey.class, "all.properties");
        ChaosMonkey chaos = new ChaosMonkey(ctx);
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
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(ChaosMonkey.class, "noProbability.properties");
        ChaosMonkey chaos = new ChaosMonkey(ctx);
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
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(ChaosMonkey.class, "noProbabilityByName.properties");
        ChaosMonkey chaos = new ChaosMonkey(ctx);
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
}

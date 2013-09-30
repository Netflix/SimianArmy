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
package com.netflix.simianarmy.chaos;

import java.util.List;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext.Notification;

// CHECKSTYLE IGNORE MagicNumberCheck
public class TestChaosMonkeyArmy {
    @Test
    public void testBurnCpu() {
        Properties properties = new Properties();
        properties.setProperty("simianarmy.chaos.enabled", "true");
        properties.setProperty("simianarmy.chaos.leashed", "false");
        properties.setProperty("simianarmy.chaos.TYPE_A.enabled", "true");
        properties.setProperty("simianarmy.chaos.notification.global.enabled", "true");

        String key = "ShutdownInstance";

        properties.setProperty("simianarmy.chaos.shutdowninstance.enabled", "true");
        properties.setProperty("simianarmy.chaos." + key.toLowerCase() + ".enabled", "true");

        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext(properties);

        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();

        List<InstanceGroup> selectedOn = ctx.selectedOn();
        List<Notification> notifications = ctx.getGloballyNotifiedList();
        
        Assert.assertEquals(selectedOn.size(), 2);
        Assert.assertEquals(selectedOn.get(0).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(0).name(), "name0");
        Assert.assertEquals(selectedOn.get(1).type(), TestChaosMonkeyContext.CrawlerTypes.TYPE_A);
        Assert.assertEquals(selectedOn.get(1).name(), "name1");

        Assert.assertEquals(notifications.size(), 2);
        Assert.assertEquals(notifications.get(0).instance, "0:i-123456780");
        Assert.assertEquals(notifications.get(0).chaosType.getKey(), key);
        Assert.assertEquals(notifications.get(1).instance, "1:i-123456781");
        Assert.assertEquals(notifications.get(1).chaosType.getKey(), key);
        
        List<String> terminated = ctx.terminated();
        Assert.assertEquals(terminated.size(), 2);
        Assert.assertEquals(terminated.get(0), "0:i-123456780");
        Assert.assertEquals(terminated.get(1), "1:i-123456781");
    }
}

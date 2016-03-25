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
package com.netflix.simianarmy.basic.chaos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import com.netflix.simianarmy.GroupType;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyScheduler;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext;
import com.netflix.simianarmy.resources.chaos.ChaosMonkeyResource;
import com.amazonaws.services.autoscaling.model.TagDescription;

// CHECKSTYLE IGNORE MagicNumberCheck
public class TestBasicChaosMonkey {
    private enum GroupTypes implements GroupType {
        TYPE_A, TYPE_B
    };

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
        Assert.assertEquals(terminated.get(0), "0:i-123456789012345670");
        Assert.assertEquals(terminated.get(1), "1:i-123456789012345671");
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
        Assert.assertEquals(terminated.get(0), "2:i-123456789012345672");
        Assert.assertEquals(terminated.get(1), "3:i-123456789012345673");
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
        Assert.assertEquals(terminated.get(0), "0:i-123456789012345670");
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
        Assert.assertEquals(terminated.get(0), "0:i-123456789012345670");
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
        Assert.assertEquals(terminated.get(0), "0:i-123456789012345670");
        Assert.assertEquals(terminated.get(1), "1:i-123456789012345671");
        Assert.assertEquals(terminated.get(2), "2:i-123456789012345672");
        Assert.assertEquals(terminated.get(3), "3:i-123456789012345673");
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
    public void testFullProbability() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("fullProbability.properties") {
            @Override
            public MonkeyScheduler scheduler() {
                return new MonkeyScheduler() {
                    @Override
                    public int frequency() {
                        return 1;
                    }

                    @Override
                    public TimeUnit frequencyUnit() {
                        return TimeUnit.DAYS;
                    }

                    @Override
                    public void start(Monkey monkey, Runnable run) {
                        Assert.assertEquals(monkey.type().name(), monkey.type().name(), "starting monkey");
                        run.run();
                    }

                    @Override
                    public void stop(Monkey monkey) {
                        Assert.assertEquals(monkey.type().name(), monkey.type().name(), "stopping monkey");
                    }
                };
            }
        };

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

    @Test
    public void testMaxTerminationCountPerDayGroupLevel() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("terminationPerDayGroupLevel.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);

        for (int i = 1; i <= 3; i++) {
            chaos.start();
            chaos.stop();
            Assert.assertEquals(ctx.selectedOn().size(), i);
            Assert.assertEquals(ctx.terminated().size(), i);
        }
        // Run the chaos the second time will NOT trigger another termination
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 3);
        Assert.assertEquals(ctx.terminated().size(), 3);
    }

    @Test
    public void testGetValueFromCfgWithDefault() {

        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("propertiesWithDefaults.properties");
        BasicChaosMonkey chaos = new BasicChaosMonkey(ctx);

        // named 1 has actual values in config
        InstanceGroup named1 = new BasicInstanceGroup("named1", GroupTypes.TYPE_A, "test-dev-1", Collections.<TagDescription>emptyList());

        // named 2 doesn't have values but it's group has values
        InstanceGroup named2 = new BasicInstanceGroup("named2", GroupTypes.TYPE_A, "test-dev-1", Collections.<TagDescription>emptyList());

        // named 3 doesn't have values and it's group doesn't have values
        InstanceGroup named3 = new BasicInstanceGroup("named3", GroupTypes.TYPE_B, "test-dev-1", Collections.<TagDescription>emptyList());

        Assert.assertEquals(chaos.getBoolFromCfgOrDefault(named1, "enabled", true), false);
        Assert.assertEquals(chaos.getNumFromCfgOrDefault(named1, "probability", 3.0), 1.1);
        Assert.assertEquals(chaos.getNumFromCfgOrDefault(named1, "maxTerminationsPerDay", 4.0), 2.1);

        Assert.assertEquals(chaos.getBoolFromCfgOrDefault(named2, "enabled", true), true);
        Assert.assertEquals(chaos.getNumFromCfgOrDefault(named2, "probability", 3.0), 1.0);
        Assert.assertEquals(chaos.getNumFromCfgOrDefault(named2, "maxTerminationsPerDay", 4.0), 2.0);

        Assert.assertEquals(chaos.getBoolFromCfgOrDefault(named3, "enabled", true), true);
        Assert.assertEquals(chaos.getNumFromCfgOrDefault(named3, "probability", 3.0), 3.0);
        Assert.assertEquals(chaos.getNumFromCfgOrDefault(named3, "maxTerminationsPerDay", 4.0), 4.0);
    }

    @Test
    public void testMandatoryTerminationDisabled() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("mandatoryTerminationDisabled.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 0);
    }

    @Test
    public void testMandatoryTerminationNotDefined() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("mandatoryTerminationNotDefined.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 0);
    }

    @Test
    public void testMandatoryTerminationNoOptInTime() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("mandatoryTerminationNoOptInTime.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 0);
    }

    @Test
    public void testMandatoryTerminationInsideWindow() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("mandatoryTerminationInsideWindow.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        // The last opt-in time is within the window, so no mandatory termination is triggered
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 0);
    }

    @Test
    public void testMandatoryTerminationOutsideWindow() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("mandatoryTerminationOutsideWindow.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        // There was no termination in the last window, so one mandatory termination is triggered
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);
    }

    @Test
    public void testMandatoryTerminationOutsideWindowWithPreviousTermination() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("mandatoryTerminationOutsideWindow.properties");
        terminateOnDemand(ctx, "TYPE_C", "name4");
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        // There was termination in the last window, so no mandatory termination is triggered
        Assert.assertEquals(ctx.selectedOn().size(), 2);
        Assert.assertEquals(ctx.terminated().size(), 1);
    }

    @Test
    public void testMandatoryTerminationInsideWindowWithPreviousTermination() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("mandatoryTerminationInsideWindow.properties");
        terminateOnDemand(ctx, "TYPE_C", "name4");
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        // There was termination in the last window, so no mandatory termination is triggered
        Assert.assertEquals(ctx.selectedOn().size(), 2);
        Assert.assertEquals(ctx.terminated().size(), 1);
    }

    @Test
    public void testNotificationEnabled() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("notificationEnabled.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 4);
        Assert.assertEquals(ctx.terminated().size(), 4);
        // Notification is enabled only for 2 terminations.
        Assert.assertEquals(ctx.getNotified(), 2);
    }

    @Test
    public void testGlobalNotificationEnabled() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("globalNotificationEnabled.properties");
        ChaosMonkey chaos = new BasicChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 4);
        Assert.assertEquals(ctx.terminated().size(), 4);
        Assert.assertEquals(ctx.getNotified(), 1);
        Assert.assertEquals(ctx.getGloballyNotified(), 4);
    }

    private void terminateOnDemand(TestChaosMonkeyContext ctx, String groupType, String groupName) {
        String input = String.format("{\"eventType\":\"CHAOS_TERMINATION\",\"groupType\":\"%s\",\"groupName\":\"%s\"}",
                groupType, groupName);

        int currentSelectedOn = ctx.selectedOn().size();
        int currentTerminated = ctx.terminated().size();

        ChaosMonkeyResource resource = new ChaosMonkeyResource(new BasicChaosMonkey(ctx));
        validateAddEventResult(resource, input, Response.Status.OK);
        Assert.assertEquals(ctx.selectedOn().size(), currentSelectedOn + 1);
        Assert.assertEquals(ctx.terminated().size(), currentTerminated + 1);
    }

    private void validateAddEventResult(ChaosMonkeyResource resource, String input, Response.Status responseStatus) {
        try {
            Response resp = resource.addEvent(input);
            Assert.assertEquals(resp.getStatus(), responseStatus.getStatusCode());
        } catch (Exception e) {
            Assert.fail("addEvent throws exception");
        }
    }
}

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

import com.amazonaws.services.autoscaling.model.TagDescription;
import org.testng.Assert;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import com.netflix.simianarmy.chaos.TestChaosMonkeyContext;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

import java.util.Collections;

public class TestCloudFormationChaosMonkey {

    public static final long EXPECTED_MILLISECONDS = 2000;

    @Test
    public void testIsGroupEnabled() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group1 = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",
                TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region", Collections.<TagDescription>emptyList());
        InstanceGroup group2 = new BasicInstanceGroup("new-group-TestGroup2-XCFNGHFNF",
                TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region", Collections.<TagDescription>emptyList());
        assertTrue(chaos.isGroupEnabled(group1));
        assertFalse(chaos.isGroupEnabled(group2));
    }

    @Test
    public void testIsMaxTerminationCountExceeded() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group1 = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",
                TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region", Collections.<TagDescription>emptyList());
        assertFalse(chaos.isMaxTerminationCountExceeded(group1));
    }

    @Test
    public void testGetEffectiveProbability() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group1 = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",
                TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region", Collections.<TagDescription>emptyList());
        assertEquals(1.0, chaos.getEffectiveProbability(group1));
    }

    @Test
    public void testNoSuffixInstanceGroup() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("disabled.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group = new BasicInstanceGroup("new-group-TestGroup-XCFNFNFNF",
                TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region", Collections.<TagDescription>emptyList());
        InstanceGroup newGroup = chaos.noSuffixInstanceGroup(group);
        assertEquals(newGroup.name(), "new-group-TestGroup");
    }

    @Test
    public void testGetLastOptInMilliseconds() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",
                TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region", Collections.<TagDescription>emptyList());
        assertEquals(chaos.getLastOptInMilliseconds(group), EXPECTED_MILLISECONDS);
    }

    @Test
    public void testCloudFormationChaosMonkeyIntegration() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);
        Assert.assertEquals(ctx.getNotified(), 1);
    }
}

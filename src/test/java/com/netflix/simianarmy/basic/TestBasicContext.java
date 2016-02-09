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

import com.amazonaws.ClientConfiguration;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestBasicContext {
    @Test
    public void testContext() {
        BasicChaosMonkeyContext ctx = new BasicChaosMonkeyContext();
        Assert.assertNotNull(ctx.scheduler());
        Assert.assertNotNull(ctx.calendar());
        Assert.assertNotNull(ctx.configuration());
        Assert.assertNotNull(ctx.cloudClient());
        Assert.assertNotNull(ctx.chaosCrawler());
        Assert.assertNotNull(ctx.chaosInstanceSelector());

        Assert.assertTrue(ctx.configuration().getBool("simianarmy.calendar.isMonkeyTime"));
        Assert.assertEquals(ctx.configuration().getStr("simianarmy.client.aws.assumeRoleArn"),
                                                        "arn:aws:iam::fakeAccount:role/fakeRole");
        // Verify that the property in chaos.properties overrides the same property in simianarmy.properties
        Assert.assertFalse(ctx.configuration().getBool("simianarmy.chaos.enabled"));
    }

    @Test
    public void testIsSafeToLogProperty() {
        BasicChaosMonkeyContext ctx = new BasicChaosMonkeyContext();
        Assert.assertTrue(ctx.isSafeToLog("simianarmy.client.aws.region"));
    }

    @Test
    public void testIsNotSafeToLogProperty() {
        BasicChaosMonkeyContext ctx = new BasicChaosMonkeyContext();
        Assert.assertFalse(ctx.isSafeToLog("simianarmy.client.aws.secretKey"));
    }

    @Test
    public void testIsNotSafeToLogVsphereProperty() {
        BasicChaosMonkeyContext ctx = new BasicChaosMonkeyContext();
        Assert.assertFalse(ctx.isSafeToLog("simianarmy.client.vsphere.password"));
    }

    @Test
    public void testIsNotUsingProxyByDefault() {
        BasicSimianArmyContext ctx = new BasicSimianArmyContext();

        ClientConfiguration awsClientConfig = ctx.getAwsClientConfig();

        Assert.assertNull(awsClientConfig.getProxyHost());
        Assert.assertEquals(awsClientConfig.getProxyPort(), -1);
        Assert.assertNull(awsClientConfig.getProxyUsername());
        Assert.assertNull(awsClientConfig.getProxyPassword());
    }

        @Test
    public void testIsAbleToUseProxyByConfiguration() {
        BasicSimianArmyContext ctx = new BasicSimianArmyContext("proxy.properties");

        ClientConfiguration awsClientConfig = ctx.getAwsClientConfig();

        Assert.assertEquals(awsClientConfig.getProxyHost(), "127.0.0.1");
        Assert.assertEquals(awsClientConfig.getProxyPort(), 80);
        Assert.assertEquals(awsClientConfig.getProxyUsername(), "fakeUser");
        Assert.assertEquals(awsClientConfig.getProxyPassword(), "fakePassword");
    }
}

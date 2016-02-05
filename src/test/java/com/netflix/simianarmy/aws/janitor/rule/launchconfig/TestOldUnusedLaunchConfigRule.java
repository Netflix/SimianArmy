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
// CHECKSTYLE IGNORE MagicNumberCheck

package com.netflix.simianarmy.aws.janitor.rule.launchconfig;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.TestUtils;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.aws.janitor.crawler.LaunchConfigJanitorCrawler;
import com.netflix.simianarmy.aws.janitor.rule.TestMonkeyCalendar;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;


public class TestOldUnusedLaunchConfigRule {

    @Test
    public void testOldUnsedLaunchConfig() {
        Resource resource = new AWSResource().withId("launchConfig1").withResourceType(AWSResourceType.LAUNCH_CONFIG);
        resource.setAdditionalField(LaunchConfigJanitorCrawler.LAUNCH_CONFIG_FIELD_USED_BY_ASG, "false");
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        int ageThreshold = 3;
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        resource.setLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        int retentionDays = 3;
        OldUnusedLaunchConfigRule rule = new OldUnusedLaunchConfigRule(calendar, ageThreshold, retentionDays);
        Assert.assertFalse(rule.isValid(resource));
        TestUtils.verifyTerminationTimeRough(resource, retentionDays, now);
    }

    @Test
    public void testOldLaunchConfigWithNullFlag() {
        Resource resource = new AWSResource().withId("launchConfig1").withResourceType(AWSResourceType.LAUNCH_CONFIG);
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        int ageThreshold = 3;
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        resource.setLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        int retentionDays = 3;
        OldUnusedLaunchConfigRule rule = new OldUnusedLaunchConfigRule(calendar, ageThreshold, retentionDays);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testUnsedLaunchConfigNotOld() {
        Resource resource = new AWSResource().withId("launchConfig1").withResourceType(AWSResourceType.LAUNCH_CONFIG);
        resource.setAdditionalField(LaunchConfigJanitorCrawler.LAUNCH_CONFIG_FIELD_USED_BY_ASG, "false");
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        int ageThreshold = 3;
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        resource.setLaunchTime(new Date(now.minusDays(ageThreshold - 1).getMillis()));
        int retentionDays = 3;
        OldUnusedLaunchConfigRule rule = new OldUnusedLaunchConfigRule(calendar, ageThreshold, retentionDays);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testUsedLaunchConfig() {
        Resource resource = new AWSResource().withId("launchConfig1").withResourceType(AWSResourceType.LAUNCH_CONFIG);
        resource.setAdditionalField(LaunchConfigJanitorCrawler.LAUNCH_CONFIG_FIELD_USED_BY_ASG, "true");
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        int ageThreshold = 3;
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        resource.setLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        int retentionDays = 3;
        OldUnusedLaunchConfigRule rule = new OldUnusedLaunchConfigRule(calendar, ageThreshold, retentionDays);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testUsedLaunchConfigNoLaunchTimeSet() {
        Resource resource = new AWSResource().withId("launchConfig1").withResourceType(AWSResourceType.LAUNCH_CONFIG);
        resource.setAdditionalField(LaunchConfigJanitorCrawler.LAUNCH_CONFIG_FIELD_USED_BY_ASG, "true");
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        int ageThreshold = 3;
        int retentionDays = 3;
        OldUnusedLaunchConfigRule rule = new OldUnusedLaunchConfigRule(calendar, ageThreshold, retentionDays);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testResourceWithExpectedTerminationTimeSet() {
        Resource resource = new AWSResource().withId("launchConfig1").withResourceType(AWSResourceType.LAUNCH_CONFIG);
        resource.setAdditionalField(LaunchConfigJanitorCrawler.LAUNCH_CONFIG_FIELD_USED_BY_ASG, "false");
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        int ageThreshold = 3;
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        resource.setLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        Date oldTermDate = new Date(now.minusDays(10).getMillis());
        String oldTermReason = "Foo";
        resource.setExpectedTerminationTime(oldTermDate);
        resource.setTerminationReason(oldTermReason);
        int retentionDays = 3;
        OldUnusedLaunchConfigRule rule = new OldUnusedLaunchConfigRule(calendar, ageThreshold, retentionDays);
        Assert.assertFalse(rule.isValid(resource));
        Assert.assertEquals(oldTermDate, resource.getExpectedTerminationTime());
        Assert.assertEquals(oldTermReason, resource.getTerminationReason());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullResource() {
        OldUnusedLaunchConfigRule rule = new OldUnusedLaunchConfigRule(new TestMonkeyCalendar(), 3, 3);
        rule.isValid(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeRetentionDays() {
        new OldUnusedLaunchConfigRule(new TestMonkeyCalendar(), -1, 60);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeLaunchConfigAgeThreshold() {
        new OldUnusedLaunchConfigRule(new TestMonkeyCalendar(), 3, -1);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullCalendar() {
        new OldUnusedLaunchConfigRule(null, 3, 60);
    }

    @Test
    public void testNonLaunchConfigResource() {
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE);
        OldUnusedLaunchConfigRule rule = new OldUnusedLaunchConfigRule(new TestMonkeyCalendar(), 3, 60);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

}

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

package com.netflix.simianarmy.aws.janitor.rule.asg;

import java.util.Date;

import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.TestUtils;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.aws.janitor.crawler.ASGJanitorCrawler;
import com.netflix.simianarmy.aws.janitor.rule.TestMonkeyCalendar;


public class TestOldEmptyASGRule {
    @Test
    public void testEmptyASGWithObsoleteLaunchConfig() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_NAME, "launchConfig");
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "0");
        int launchConfiguAgeThreshold = 60;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_CREATION_TIME,
                String.valueOf(now.minusDays(launchConfiguAgeThreshold + 1).getMillis()));
        int retentionDays = 3;
        OldEmptyASGRule rule = new OldEmptyASGRule(calendar, launchConfiguAgeThreshold, retentionDays,
                new DummyASGInstanceValidator());
        Assert.assertFalse(rule.isValid(resource));
        TestUtils.verifyTerminationTimeRough(resource, retentionDays, now);
    }

    @Test
    public void testEmptyASGWithValidLaunchConfig() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_NAME, "launchConfig");
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "0");
        int launchConfiguAgeThreshold = 60;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_CREATION_TIME,
                String.valueOf(now.minusDays(launchConfiguAgeThreshold - 1).getMillis()));
        int retentionDays = 3;
        OldEmptyASGRule rule = new OldEmptyASGRule(calendar, launchConfiguAgeThreshold, retentionDays,
                new DummyASGInstanceValidator());
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testASGWithInstances() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_NAME, "launchConfig");
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "2");
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_INSTANCES, "123456789012345671,i-123456789012345672");
        int launchConfiguAgeThreshold = 60;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_CREATION_TIME,
                String.valueOf(now.minusDays(launchConfiguAgeThreshold + 1).getMillis()));
        int retentionDays = 3;
        OldEmptyASGRule rule = new OldEmptyASGRule(calendar, launchConfiguAgeThreshold, retentionDays,
                new DummyASGInstanceValidator());
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testASGWithoutInstanceAndNonZeroSize() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_NAME, "launchConfig");
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "2");
        int launchConfiguAgeThreshold = 60;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_CREATION_TIME,
                String.valueOf(now.minusDays(launchConfiguAgeThreshold + 1).getMillis()));
        int retentionDays = 3;
        OldEmptyASGRule rule = new OldEmptyASGRule(calendar, launchConfiguAgeThreshold, retentionDays,
                new DummyASGInstanceValidator());
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testEmptyASGWithoutLaunchConfig() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "0");
        int launchConfiguAgeThreshold = 60;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        int retentionDays = 3;
        OldEmptyASGRule rule = new OldEmptyASGRule(calendar, launchConfiguAgeThreshold, retentionDays,
                new DummyASGInstanceValidator());
        Assert.assertFalse(rule.isValid(resource));
        TestUtils.verifyTerminationTimeRough(resource, retentionDays, now);
    }

    @Test
    public void testEmptyASGWithLaunchConfigWithoutCreateTime() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_NAME, "launchConfig");
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "0");
        int launchConfiguAgeThreshold = 60;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        int retentionDays = 3;
        OldEmptyASGRule rule = new OldEmptyASGRule(calendar, launchConfiguAgeThreshold, retentionDays,
                new DummyASGInstanceValidator());
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testResourceWithExpectedTerminationTimeSet() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "0");
        int launchConfiguAgeThreshold = 60;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        int retentionDays = 3;
        OldEmptyASGRule rule = new OldEmptyASGRule(calendar, launchConfiguAgeThreshold, retentionDays,
                new DummyASGInstanceValidator());
        Date oldTermDate = new Date(now.plusDays(10).getMillis());
        String oldTermReason = "Foo";
        resource.setExpectedTerminationTime(oldTermDate);
        resource.setTerminationReason(oldTermReason);
        Assert.assertFalse(rule.isValid(resource));
        Assert.assertEquals(oldTermDate, resource.getExpectedTerminationTime());
        Assert.assertEquals(oldTermReason, resource.getTerminationReason());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullValidator() {
        new OldEmptyASGRule(new TestMonkeyCalendar(), 3, 60, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullResource() {
        OldEmptyASGRule rule = new OldEmptyASGRule(new TestMonkeyCalendar(), 3, 60, new DummyASGInstanceValidator());
        rule.isValid(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeRetentionDays() {
        new OldEmptyASGRule(new TestMonkeyCalendar(), -1, 60, new DummyASGInstanceValidator());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeLaunchConfigAgeThreshold() {
        new OldEmptyASGRule(new TestMonkeyCalendar(), 3, -1, new DummyASGInstanceValidator());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullCalendar() {
        new OldEmptyASGRule(null, 3, 60, new DummyASGInstanceValidator());
    }

    @Test
    public void testNonASGResource() {
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE);
        OldEmptyASGRule rule = new OldEmptyASGRule(new TestMonkeyCalendar(), 3, 60, new DummyASGInstanceValidator());
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

}

//CHECKSTYLE IGNORE Javadoc
//CHECKSTYLE IGNORE MagicNumberCheck
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

package com.netflix.simianarmy.aws.janitor.rule.asg;

import java.util.Date;

import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.aws.janitor.crawler.ASGJanitorCrawler;
import com.netflix.simianarmy.aws.janitor.rule.TestMonkeyCalendar;


public class TestSuspendedASGRule {
    @Test
    public void testEmptyASGSuspendedMoreThanThreshold() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "0");
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        int suspensionAgeThreshold = 2;
        DateTime suspensionTime = now.minusDays(suspensionAgeThreshold + 1);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_SUSPENSION_TIME,
                ASGJanitorCrawler.SUSPENSION_TIME_FORMATTER.print(suspensionTime));
        int retentionDays = 3;
        SuspendedASGRule rule = new SuspendedASGRule(calendar, suspensionAgeThreshold, retentionDays, null);
        Assert.assertFalse(rule.isValid(resource));
        verifyTerminationTime(resource, retentionDays, now);
    }

    @Test
    public void testEmptyASGSuspendedLessThanThreshold() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_NAME, "launchConfig");
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "0");
        int suspensionAgeThreshold = 2;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_CREATION_TIME,
                String.valueOf(now.minusDays(suspensionAgeThreshold + 1).getMillis()));
        int retentionDays = 3;
        SuspendedASGRule rule = new SuspendedASGRule(calendar, suspensionAgeThreshold, retentionDays, null);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testASGWithInstances() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "2");
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_INSTANCES, "i-1,i-2");
        int suspensionAgeThreshold = 2;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        DateTime suspensionTime = now.minusDays(suspensionAgeThreshold + 1);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_SUSPENSION_TIME,
                ASGJanitorCrawler.SUSPENSION_TIME_FORMATTER.print(suspensionTime));
        int retentionDays = 3;
        SuspendedASGRule rule = new SuspendedASGRule(calendar, suspensionAgeThreshold, retentionDays, null);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testASGWithoutInstanceAndNonZeroSize() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "2");
        int suspensionAgeThreshold = 2;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        DateTime suspensionTime = now.minusDays(suspensionAgeThreshold + 1);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_SUSPENSION_TIME,
                ASGJanitorCrawler.SUSPENSION_TIME_FORMATTER.print(suspensionTime));
        int retentionDays = 3;
        SuspendedASGRule rule = new SuspendedASGRule(calendar, suspensionAgeThreshold, retentionDays, null);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testEmptyASGNotSuspended() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "0");
        int suspensionAgeThreshold = 2;
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        int retentionDays = 3;
        SuspendedASGRule rule = new SuspendedASGRule(calendar, suspensionAgeThreshold, retentionDays, null);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testResourceWithExpectedTerminationTimeSet() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "0");
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        int suspensionAgeThreshold = 2;
        DateTime suspensionTime = now.minusDays(suspensionAgeThreshold + 1);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_SUSPENSION_TIME,
                ASGJanitorCrawler.SUSPENSION_TIME_FORMATTER.print(suspensionTime));
        int retentionDays = 3;
        SuspendedASGRule rule = new SuspendedASGRule(calendar, suspensionAgeThreshold, retentionDays, null);
        Date oldTermDate = new Date(now.plusDays(10).getMillis());
        String oldTermReason = "Foo";
        resource.setExpectedTerminationTime(oldTermDate);
        resource.setTerminationReason(oldTermReason);
        Assert.assertFalse(rule.isValid(resource));
        Assert.assertEquals(oldTermDate, resource.getExpectedTerminationTime());
        Assert.assertEquals(oldTermReason, resource.getTerminationReason());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullResource() {
        SuspendedASGRule rule = new SuspendedASGRule(new TestMonkeyCalendar(), 3, 2, null);
        rule.isValid(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeRetentionDays() {
        new SuspendedASGRule(new TestMonkeyCalendar(), -1, 2, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeLaunchConfigAgeThreshold() {
        new SuspendedASGRule(new TestMonkeyCalendar(), 3, -1, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullCalendar() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE, "0");
        MonkeyCalendar calendar = new TestMonkeyCalendar();
        int suspensionAgeThreshold = 2;
        resource.setAdditionalField(ASGJanitorCrawler.ASG_FIELD_SUSPENSION_TIME, "foo");
        int retentionDays = 3;
        SuspendedASGRule rule = new SuspendedASGRule(calendar, suspensionAgeThreshold, retentionDays, null);
        Assert.assertFalse(rule.isValid(resource));
    }

    @Test
    public void testNonASGResource() {
        Resource resource = new AWSResource().withId("i-12345678").withResourceType(AWSResourceType.INSTANCE);
        SuspendedASGRule rule = new SuspendedASGRule(new TestMonkeyCalendar(), 3, 2, null);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSuspensionTimeIncorrectFormat() {
        new SuspendedASGRule(null, 3, 2, null);
    }

    /** Verify that the termination date is roughly rentionDays from now **/
    private void verifyTerminationTime(Resource resource, int retentionDays, DateTime now) {
        long days = (resource.getExpectedTerminationTime().getTime() - now.getMillis()) / (24 * 60 * 60 * 1000);
        Assert.assertEquals(days, retentionDays);
    }
}

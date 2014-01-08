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

package com.netflix.simianarmy.aws.janitor.rule.snapshot;

import java.util.Date;

import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.aws.janitor.crawler.EBSSnapshotJanitorCrawler;
import com.netflix.simianarmy.aws.janitor.rule.TestMonkeyCalendar;
import com.netflix.simianarmy.janitor.JanitorMonkey;


public class TestNoGeneratedAMIRule {

    @Test
    public void testNonSnapshotResource() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        ((AWSResource) resource).setAWSResourceState("completed");
        NoGeneratedAMIRule rule = new NoGeneratedAMIRule(new TestMonkeyCalendar(), 0, 0);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testUncompletedVolume() {
        Resource resource = new AWSResource().withId("snap123").withResourceType(AWSResourceType.EBS_SNAPSHOT);
        ((AWSResource) resource).setAWSResourceState("stopped");
        NoGeneratedAMIRule rule = new NoGeneratedAMIRule(new TestMonkeyCalendar(), 0, 0);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testTaggedAsNotMark() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("snap123").withResourceType(AWSResourceType.EBS_SNAPSHOT)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("completed");
        int retentionDays = 4;
        NoGeneratedAMIRule rule = new NoGeneratedAMIRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        resource.setTag(JanitorMonkey.JANITOR_TAG, "donotmark");
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testUserSpecifiedTerminationDate() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("snap123").withResourceType(AWSResourceType.EBS_SNAPSHOT)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("completed");
        int retentionDays = 4;
        DateTime userDate = new DateTime(now.plusDays(3).withTimeAtStartOfDay());
        resource.setTag(JanitorMonkey.JANITOR_TAG,
                NoGeneratedAMIRule.TERMINATION_DATE_FORMATTER.print(userDate));
        NoGeneratedAMIRule rule = new NoGeneratedAMIRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        Assert.assertFalse(rule.isValid(resource));
        Assert.assertEquals(resource.getExpectedTerminationTime().getTime(), userDate.getMillis());
    }

    @Test
    public void testOldSnapshotWithoutAMI() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("snap123").withResourceType(AWSResourceType.EBS_SNAPSHOT)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("completed");
        int retentionDays = 4;
        NoGeneratedAMIRule rule = new NoGeneratedAMIRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        Assert.assertFalse(rule.isValid(resource));
        verifyTerminationTime(resource, retentionDays, now);
    }

    @Test
    public void testSnapshotWithoutAMINotOld() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("snap123").withResourceType(AWSResourceType.EBS_SNAPSHOT)
                .withLaunchTime(new Date(now.minusDays(ageThreshold - 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("completed");
        int retentionDays = 4;
        NoGeneratedAMIRule rule = new NoGeneratedAMIRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testWithAMIs() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("snap123").withResourceType(AWSResourceType.EBS_SNAPSHOT)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("completed");
        resource.setAdditionalField(EBSSnapshotJanitorCrawler.SNAPSHOT_FIELD_AMIS, "ami-123");
        int retentionDays = 4;
        NoGeneratedAMIRule rule = new NoGeneratedAMIRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testSnapshotsWithoutLauchTime() {
        int ageThreshold = 5;
        Resource resource = new AWSResource().withId("snap123").withResourceType(AWSResourceType.EBS_SNAPSHOT);
        ((AWSResource) resource).setAWSResourceState("completed");
        int retentionDays = 4;
        NoGeneratedAMIRule rule = new NoGeneratedAMIRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }


    @Test
    public void testResourceWithExpectedTerminationTimeSet() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("snap123").withResourceType(AWSResourceType.EBS_SNAPSHOT)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("completed");
        Date oldTermDate = new Date(now.plusDays(10).getMillis());
        String oldTermReason = "Foo";
        int retentionDays = 4;
        NoGeneratedAMIRule rule = new NoGeneratedAMIRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        resource.setExpectedTerminationTime(oldTermDate);
        resource.setTerminationReason(oldTermReason);
        Assert.assertFalse(rule.isValid(resource));
        Assert.assertEquals(oldTermDate, resource.getExpectedTerminationTime());
        Assert.assertEquals(oldTermReason, resource.getTerminationReason());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullResource() {
        NoGeneratedAMIRule rule = new NoGeneratedAMIRule(new TestMonkeyCalendar(), 5, 4);
        rule.isValid(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeAgeThreshold() {
        new NoGeneratedAMIRule(new TestMonkeyCalendar(), -1, 4);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeRetentionDaysWithOwner() {
        new NoGeneratedAMIRule(new TestMonkeyCalendar(), 5, -4);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullCalendar() {
        new NoGeneratedAMIRule(null, 5, 4);
    }

    /** Verify that the termination date is roughly rentionDays from now **/
    private void verifyTerminationTime(Resource resource, int retentionDays, DateTime now) {
        long days = (resource.getExpectedTerminationTime().getTime() - now.getMillis()) / (24 * 60 * 60 * 1000);
        Assert.assertEquals(days, retentionDays);
    }
}

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

package com.netflix.simianarmy.aws.janitor.rule.instance;

import java.util.Date;

import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.TestUtils;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.aws.janitor.crawler.InstanceJanitorCrawler;
import com.netflix.simianarmy.aws.janitor.rule.TestMonkeyCalendar;

public class TestOrphanedInstanceRule {

    @Test
    public void testOrphanedInstancesWithOwner() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()))
                .withOwnerEmail("owner@foo.com");
        ((AWSResource) resource).setAWSResourceState("running");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        OrphanedInstanceRule rule = new OrphanedInstanceRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertFalse(rule.isValid(resource));
        TestUtils.verifyTerminationTimeRough(resource, retentionDaysWithOwner, now);
    }

    @Test
    public void testOrphanedInstancesWithoutOwner() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("running");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        OrphanedInstanceRule rule = new OrphanedInstanceRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertFalse(rule.isValid(resource));
        TestUtils.verifyTerminationTimeRough(resource, retentionDaysWithoutOwner, now);
    }

    @Test
    public void testOrphanedInstancesWithoutLaunchTime() {
        int ageThreshold = 5;
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE);
        ((AWSResource) resource).setAWSResourceState("running");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        OrphanedInstanceRule rule = new OrphanedInstanceRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testOrphanedInstancesWithLaunchTimeNotExpires() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE)
                .withLaunchTime(new Date(now.minusDays(ageThreshold - 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("running");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        OrphanedInstanceRule rule = new OrphanedInstanceRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testNonOrphanedInstances() {
        int ageThreshold = 5;
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE)
                .setAdditionalField(InstanceJanitorCrawler.INSTANCE_FIELD_ASG_NAME, "asg1");
        ((AWSResource) resource).setAWSResourceState("running");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        OrphanedInstanceRule rule = new OrphanedInstanceRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testResourceWithExpectedTerminationTimeSet() {
        DateTime now = DateTime.now();
        Date oldTermDate = new Date(now.plusDays(10).getMillis());
        String oldTermReason = "Foo";
        int ageThreshold = 5;
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()))
                .withExpectedTerminationTime(oldTermDate)
                .withTerminationReason(oldTermReason);
        ((AWSResource) resource).setAWSResourceState("running");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        OrphanedInstanceRule rule = new OrphanedInstanceRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertFalse(rule.isValid(resource));
        Assert.assertEquals(oldTermDate, resource.getExpectedTerminationTime());
        Assert.assertEquals(oldTermReason, resource.getTerminationReason());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullResource() {
        OrphanedInstanceRule rule = new OrphanedInstanceRule(new TestMonkeyCalendar(), 5, 4, 8);
        rule.isValid(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeAgeThreshold() {
        new OrphanedInstanceRule(new TestMonkeyCalendar(), -1, 4, 8);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeRetentionDaysWithOwner() {
        new OrphanedInstanceRule(new TestMonkeyCalendar(), 5, -4, 8);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeRetentionDaysWithoutOwner() {
        new OrphanedInstanceRule(new TestMonkeyCalendar(), 5, 4, -8);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullCalendar() {
        new OrphanedInstanceRule(null, 5, 4, 8);
    }

    @Test
    public void testNonInstanceResource() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        ((AWSResource) resource).setAWSResourceState("running");
        OrphanedInstanceRule rule = new OrphanedInstanceRule(new TestMonkeyCalendar(), 0, 0, 0);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testNonRunningInstance() {
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE);
        ((AWSResource) resource).setAWSResourceState("stopping");
        OrphanedInstanceRule rule = new OrphanedInstanceRule(new TestMonkeyCalendar(), 0, 0, 0);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

}

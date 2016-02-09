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

package com.netflix.simianarmy.aws.janitor.rule.generic;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.TestUtils;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.aws.janitor.crawler.InstanceJanitorCrawler;
import com.netflix.simianarmy.aws.janitor.rule.TestMonkeyCalendar;

public class TestUntaggedRule {

    @Test
    public void testUntaggedInstanceWithOwner() {
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE)
                .withOwnerEmail("owner@foo.com");
        resource.setTag("tag1", "value1");
        ((AWSResource) resource).setAWSResourceState("running");
        Set<String> tags = new HashSet<String>();
        tags.add("tag1");
        tags.add("tag2");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        UntaggedRule rule = new UntaggedRule(new TestMonkeyCalendar(), tags, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertFalse(rule.isValid(resource));
        TestUtils.verifyTerminationTimeRough(resource, retentionDaysWithOwner, now);
    }

    @Test
    public void testUntaggedInstanceWithoutOwner() {
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE);
        resource.setTag("tag1", "value1");
        ((AWSResource) resource).setAWSResourceState("running");
        Set<String> tags = new HashSet<String>();
        tags.add("tag1");
        tags.add("tag2");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        UntaggedRule rule = new UntaggedRule(new TestMonkeyCalendar(), tags, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertFalse(rule.isValid(resource));
        TestUtils.verifyTerminationTimeRough(resource, retentionDaysWithoutOwner, now);
    }

    @Test
    public void testTaggedInstance() {
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE);
        resource.setTag("tag1", "value1");
        resource.setTag("tag2", "value2");
        ((AWSResource) resource).setAWSResourceState("running");
        Set<String> tags = new HashSet<String>();
        tags.add("tag1");
        tags.add("tag2");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        UntaggedRule rule = new UntaggedRule(new TestMonkeyCalendar(), tags, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertTrue(rule.isValid(resource));
    }

    @Test
    public void testUntaggedResource() {
        DateTime now = DateTime.now();
        Resource imageResource = new AWSResource().withId("ami-123123").withResourceType(AWSResourceType.IMAGE);
        Resource asgResource = new AWSResource().withId("my-cool-asg").withResourceType(AWSResourceType.ASG);
        Resource ebsSnapshotResource = new AWSResource().withId("snap-12345678901234567").withResourceType(AWSResourceType.EBS_SNAPSHOT);
        Resource lauchConfigurationResource = new AWSResource().withId("my-cool-launch-configuration").withResourceType(AWSResourceType.LAUNCH_CONFIG);
        Set<String> tags = new HashSet<String>();
        tags.add("tag1");
        tags.add("tag2");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        UntaggedRule rule = new UntaggedRule(new TestMonkeyCalendar(), tags, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertFalse(rule.isValid(imageResource));
        Assert.assertFalse(rule.isValid(asgResource));
        Assert.assertFalse(rule.isValid(ebsSnapshotResource));
        Assert.assertFalse(rule.isValid(lauchConfigurationResource));
        TestUtils.verifyTerminationTimeRough(imageResource, retentionDaysWithoutOwner, now);
        TestUtils.verifyTerminationTimeRough(asgResource, retentionDaysWithoutOwner, now);
        TestUtils.verifyTerminationTimeRough(ebsSnapshotResource, retentionDaysWithoutOwner, now);
        TestUtils.verifyTerminationTimeRough(lauchConfigurationResource, retentionDaysWithoutOwner, now);
    }

    @Test
    public void testResourceWithExpectedTerminationTimeSet() {
        DateTime now = DateTime.now();
        Date oldTermDate = new Date(now.plusDays(10).getMillis());
        String oldTermReason = "Foo";
        Resource resource = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE)
                .withExpectedTerminationTime(oldTermDate)
                .withTerminationReason(oldTermReason);
        ((AWSResource) resource).setAWSResourceState("running");
        Set<String> tags = new HashSet<String>();
        tags.add("tag1");
        tags.add("tag2");
        int retentionDaysWithOwner = 4;
        int retentionDaysWithoutOwner = 8;
        UntaggedRule rule = new UntaggedRule(new TestMonkeyCalendar(), tags, retentionDaysWithOwner, retentionDaysWithoutOwner);
        Assert.assertFalse(rule.isValid(resource));
        Assert.assertEquals(oldTermDate, resource.getExpectedTerminationTime());
        Assert.assertEquals(oldTermReason, resource.getTerminationReason());
    }

}

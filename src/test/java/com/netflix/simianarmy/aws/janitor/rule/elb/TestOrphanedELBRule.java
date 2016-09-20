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

package com.netflix.simianarmy.aws.janitor.rule.elb;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.TestUtils;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.aws.janitor.rule.TestMonkeyCalendar;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestOrphanedELBRule {

    @Test
    public void testELBWithNoInstancesNoASGs() {
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("test-elb").withResourceType(AWSResourceType.ELB)
                .withOwnerEmail("owner@foo.com");
        resource.setAdditionalField("referencedASGCount", "0");
        resource.setAdditionalField("instanceCount", "0");
        OrphanedELBRule rule = new OrphanedELBRule(new TestMonkeyCalendar(), 7);
        Assert.assertFalse(rule.isValid(resource));
        TestUtils.verifyTerminationTimeRough(resource, 7, now);
    }

    @Test
    public void testELBWithInstancesNoASGs() {
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("test-elb").withResourceType(AWSResourceType.ELB)
                .withOwnerEmail("owner@foo.com");
        resource.setAdditionalField("referencedASGCount", "0");
        resource.setAdditionalField("instanceCount", "4");
        OrphanedELBRule rule = new OrphanedELBRule(new TestMonkeyCalendar(), 7);
        Assert.assertTrue(rule.isValid(resource));
    }

    @Test
    public void testELBWithReferencedASGsNoInstances() {
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("test-elb").withResourceType(AWSResourceType.ELB)
                .withOwnerEmail("owner@foo.com");
        resource.setAdditionalField("referencedASGCount", "4");
        resource.setAdditionalField("instanceCount", "0");
        OrphanedELBRule rule = new OrphanedELBRule(new TestMonkeyCalendar(), 7);
        Assert.assertTrue(rule.isValid(resource));
    }

    @Test
    public void testMissingInstanceCountCheck() {
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("test-elb").withResourceType(AWSResourceType.ELB)
                .withOwnerEmail("owner@foo.com");
        resource.setAdditionalField("referencedASGCount", "0");
        OrphanedELBRule rule = new OrphanedELBRule(new TestMonkeyCalendar(), 7);
        Assert.assertTrue(rule.isValid(resource));
    }

    @Test
    public void testMissingReferencedASGCountCheck() {
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("test-elb").withResourceType(AWSResourceType.ELB)
                .withOwnerEmail("owner@foo.com");
        resource.setAdditionalField("instanceCount", "0");
        OrphanedELBRule rule = new OrphanedELBRule(new TestMonkeyCalendar(), 7);
        Assert.assertTrue(rule.isValid(resource));
    }

    @Test
    public void testMissingCountsCheck() {
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("test-elb").withResourceType(AWSResourceType.ELB)
                .withOwnerEmail("owner@foo.com");
        OrphanedELBRule rule = new OrphanedELBRule(new TestMonkeyCalendar(), 7);
        Assert.assertTrue(rule.isValid(resource));
    }

    @Test
    public void testMissingCountsCheckWithExtraFields() {
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("test-elb").withResourceType(AWSResourceType.ELB)
                .withOwnerEmail("owner@foo.com");
        resource.setAdditionalField("bogusField1", "0");
        resource.setAdditionalField("bogusField2", "0");
        OrphanedELBRule rule = new OrphanedELBRule(new TestMonkeyCalendar(), 7);
        Assert.assertTrue(rule.isValid(resource));
    }

}

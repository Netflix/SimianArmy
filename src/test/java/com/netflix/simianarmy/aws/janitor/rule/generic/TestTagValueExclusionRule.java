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

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;

public class TestTagValueExclusionRule {

    HashMap<String,String> exclusionTags = null;

    @BeforeTest
    public void beforeTest() {
        exclusionTags = new HashMap<>();
        exclusionTags.put("tag1", "excludeme");
        exclusionTags.put("tag2", "excludeme2");
    }

    @Test
    public void testExcludeTaggedResourceWithTagAndValueMatch1() {
        Resource r1 = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE).withOwnerEmail("owner@foo.com");
        r1.setTag("tag", null);
        r1.setTag("tag1", "excludeme");
        r1.setTag("tag2", "somethingelse");

        TagValueExclusionRule rule = new TagValueExclusionRule(exclusionTags);
        Assert.assertTrue(rule.isValid(r1));
    }

    @Test
    public void testExcludeTaggedResourceWithTagAndValueMatch2() {
        Resource r1 = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE).withOwnerEmail("owner@foo.com");
        r1.setTag("tag", null);
        r1.setTag("tag1", "somethingelse");
        r1.setTag("tag2", "excludeme2");

        TagValueExclusionRule rule = new TagValueExclusionRule(exclusionTags);
        Assert.assertTrue(rule.isValid(r1));
    }

    @Test
    public void testExcludeTaggedResourceWithTagAndValueMatchBoth() {
        Resource r1 = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE).withOwnerEmail("owner@foo.com");
        r1.setTag("tag", null);
        r1.setTag("tag1", "excludeme");
        r1.setTag("tag2", "excludeme2");

        TagValueExclusionRule rule = new TagValueExclusionRule(exclusionTags);
        Assert.assertTrue(rule.isValid(r1));
    }

    @Test
    public void testExcludeTaggedResourceTagMatchOnly() {
        Resource r1 = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE).withOwnerEmail("owner@foo.com");
        r1.setTag("tag", null);
        r1.setTag("tag1", "somethingelse");
        r1.setTag("tag2", "somethingelse2");

        TagValueExclusionRule rule = new TagValueExclusionRule(exclusionTags);
        Assert.assertFalse(rule.isValid(r1));
    }

    @Test
    public void testExcludeTaggedResourceAllNullTags() {
        Resource r1 = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE).withOwnerEmail("owner@foo.com");
        r1.setTag("tag", null);
        r1.setTag("tag1", null);
        r1.setTag("tag2", null);

        TagValueExclusionRule rule = new TagValueExclusionRule(exclusionTags);
        Assert.assertFalse(rule.isValid(r1));
    }

    @Test
    public void testExcludeTaggedResourceValueMatchOnly() {
        Resource r1 = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE).withOwnerEmail("owner@foo.com");
        r1.setTag("tag", null);
        r1.setTag("tagA", "excludeme");
        r1.setTag("tagB", "excludeme2");

        TagValueExclusionRule rule = new TagValueExclusionRule(exclusionTags);
        Assert.assertFalse(rule.isValid(r1));
    }

    @Test
    public void testExcludeUntaggedResource() {
        Resource r1 = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE).withOwnerEmail("owner@foo.com");

        TagValueExclusionRule rule = new TagValueExclusionRule(exclusionTags);
        Assert.assertFalse(rule.isValid(r1));
    }

    @Test
    public void testNameValueConstructor() {
        Resource r1 = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE).withOwnerEmail("owner@foo.com");
        r1.setTag("tag1", "excludeme");

        String names = "tag1";
        String vals  = "excludeme";
        TagValueExclusionRule rule = new TagValueExclusionRule(names.split(","), vals.split(","));
        Assert.assertTrue(rule.isValid(r1));
    }

    @Test
    public void testNameValueConstructor2() {
        Resource r1 = new AWSResource().withId("i-12345678901234567").withResourceType(AWSResourceType.INSTANCE).withOwnerEmail("owner@foo.com");
        r1.setTag("tag1", "excludeme");

        String names = "tag1,tag2";
        String vals  = "excludeme,excludeme2";
        TagValueExclusionRule rule = new TagValueExclusionRule(names.split(","), vals.split(","));
        Assert.assertTrue(rule.isValid(r1));
    }
}

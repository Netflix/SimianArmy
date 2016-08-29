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
package com.netflix.simianarmy.basic.janitor;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.janitor.Rule;
import org.joda.time.DateTime;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;

public class TestBasicJanitorRuleEngine {

    @Test
    public void testEmptyRuleSet() {
        Resource resource = new AWSResource().withId("id");
        BasicJanitorRuleEngine engine = new BasicJanitorRuleEngine();
        Assert.assertTrue(engine.isValid(resource));
    }

    @Test
    public void testAllValid() {
        Resource resource = new AWSResource().withId("id");
        BasicJanitorRuleEngine engine = new BasicJanitorRuleEngine()
        .addRule(new AlwaysValidRule())
        .addRule(new AlwaysValidRule())
        .addRule(new AlwaysValidRule());
        Assert.assertTrue(engine.isValid(resource));
    }

    @Test
    public void testMixed() {
        Resource resource = new AWSResource().withId("id");
        DateTime now = DateTime.now();
        BasicJanitorRuleEngine engine = new BasicJanitorRuleEngine()
        .addRule(new AlwaysValidRule())
        .addRule(new AlwaysInvalidRule(now, 1))
        .addRule(new AlwaysValidRule());
        Assert.assertFalse(engine.isValid(resource));
    }

    @Test
    public void testIsValidWithNearestTerminationTime() {
        int[][] permutaions = {{1, 2, 3}, {1, 3, 2}, {2, 1, 3}, {2, 3, 1}, {3, 1, 2}, {3, 2, 1}};

        for (int[] perm : permutaions) {
            Resource resource = new AWSResource().withId("id");
            DateTime now = DateTime.now();
            BasicJanitorRuleEngine engine = new BasicJanitorRuleEngine()
            .addRule(new AlwaysInvalidRule(now, perm[0]))
            .addRule(new AlwaysInvalidRule(now, perm[1]))
            .addRule(new AlwaysInvalidRule(now, perm[2]));
            Assert.assertFalse(engine.isValid(resource));
            Assert.assertEquals(
                    resource.getExpectedTerminationTime().getTime(),
                    now.plusDays(1).getMillis());
            Assert.assertEquals(resource.getTerminationReason(), "1");
        }
    }

    @Test void testWithExclusionRuleMatch1() {
        Resource resource = new AWSResource().withId("id");
        DateTime now = DateTime.now();
        BasicJanitorRuleEngine engine = new BasicJanitorRuleEngine()
                .addExclusionRule(new AlwaysValidRule())
                .addRule(new AlwaysInvalidRule(now, 1));
        Assert.assertTrue(engine.isValid(resource));
    }

    @Test void testWithExclusionRuleMatch2() {
        Resource resource = new AWSResource().withId("id");
        DateTime now = DateTime.now();
        BasicJanitorRuleEngine engine = new BasicJanitorRuleEngine()
                .addExclusionRule(new AlwaysValidRule())
                .addRule(new AlwaysValidRule());
        Assert.assertTrue(engine.isValid(resource));
    }

    @Test void testWithExclusionRuleNotMatch1() {
        Resource resource = new AWSResource().withId("id");
        DateTime now = DateTime.now();
        BasicJanitorRuleEngine engine = new BasicJanitorRuleEngine()
                .addExclusionRule(new AlwaysInvalidRule(now, 1))
                .addRule(new AlwaysInvalidRule(now, 1));
        Assert.assertFalse(engine.isValid(resource));
    }

    @Test void testWithExclusionRuleNotMatch2() {
        Resource resource = new AWSResource().withId("id");
        DateTime now = DateTime.now();
        BasicJanitorRuleEngine engine = new BasicJanitorRuleEngine()
                .addExclusionRule(new AlwaysInvalidRule(now, 1))
                .addRule(new AlwaysValidRule());
        Assert.assertTrue(engine.isValid(resource));
    }
}

class AlwaysValidRule implements Rule {
    @Override
    public boolean isValid(Resource resource) {
        return true;
    }
}

class AlwaysInvalidRule implements Rule {
    private final int retentionDays;
    private final DateTime now;

    public AlwaysInvalidRule(DateTime now, int retentionDays) {
        this.retentionDays = retentionDays;
        this.now = now;
    }

    @Override
    public boolean isValid(Resource resource) {
        resource.setExpectedTerminationTime(
                new Date(now.plusDays(retentionDays).getMillis()));
        resource.setTerminationReason(String.valueOf(retentionDays));
        return false;
    }
}

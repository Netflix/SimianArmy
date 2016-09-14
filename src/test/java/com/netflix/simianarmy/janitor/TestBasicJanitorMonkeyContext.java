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
package com.netflix.simianarmy.janitor;

import com.netflix.simianarmy.aws.janitor.rule.generic.UntaggedRule;
import com.netflix.simianarmy.basic.TestBasicCalendar;
import com.netflix.simianarmy.basic.janitor.BasicJanitorRuleEngine;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The basic implementation of the context class for Janitor monkey.
 */
public class TestBasicJanitorMonkeyContext {
    
    private static final int SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_RETENTIONDAYSWITHOWNER = 3;
    
    private static final int SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_RETENTIONDAYSWITHOUTOWNER = 8;
    
    private static final Boolean SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_ENABLED = true;
    
    private static final Set<String> SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_REQUIREDTAGS = new HashSet<String>(Arrays.asList("owner", "costcenter"));
    
    private static final String SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_RESOURCES = "Instance";
    
    private String monkeyRegion;

    private TestBasicCalendar monkeyCalendar;

    public TestBasicJanitorMonkeyContext() {
        super();
    }
    
    @BeforeMethod
    public void before() {
        monkeyRegion = "us-east-1";
        monkeyCalendar = new TestBasicCalendar();
    }
    
    @Test
    public void testAddRuleWithUntaggedRuleResource() {
        JanitorRuleEngine ruleEngine = new BasicJanitorRuleEngine();
        Boolean untaggedRuleEnabled = new Boolean(true);

        Rule rule = new UntaggedRule(monkeyCalendar, SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_REQUIREDTAGS,
                SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_RETENTIONDAYSWITHOWNER,
                        SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_RETENTIONDAYSWITHOUTOWNER);
        if (untaggedRuleEnabled && getUntaggedRuleResourceSet().contains("INSTANCE")) {
            ruleEngine.addRule(rule);
        }
        Assert.assertTrue(ruleEngine.getRules().contains(rule));
    }

    @Test
    public void testAddRuleWithoutUntaggedRuleResource() {
        JanitorRuleEngine ruleEngine = new BasicJanitorRuleEngine();
        Boolean untaggedRuleEnabled = new Boolean(true);

        Rule rule = new UntaggedRule(monkeyCalendar, SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_REQUIREDTAGS,
                SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_RETENTIONDAYSWITHOWNER,
                        SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_RETENTIONDAYSWITHOUTOWNER);
        if (untaggedRuleEnabled && getUntaggedRuleResourceSet().contains("ASG")) {
            ruleEngine.addRule(rule);
        }
        Assert.assertFalse(ruleEngine.getRules().contains(rule));
    }

    private Set<String> getUntaggedRuleResourceSet() {
        Set<String> untaggedRuleResourceSet = new HashSet<String>();
        if (SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_ENABLED) {
            String untaggedRuleResources = SIMIANARMY_JANITOR_RULE_UNTAGGEDRULE_RESOURCES;
            if (StringUtils.isNotBlank(untaggedRuleResources)) {
                for (String resourceType : untaggedRuleResources.split(",")) {
                    untaggedRuleResourceSet.add(resourceType.trim().toUpperCase());
                }
            }
        }
        return untaggedRuleResourceSet;
    }
}

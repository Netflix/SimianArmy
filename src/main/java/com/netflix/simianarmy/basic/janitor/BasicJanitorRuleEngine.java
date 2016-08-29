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

package com.netflix.simianarmy.basic.janitor;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.janitor.JanitorRuleEngine;
import com.netflix.simianarmy.janitor.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Basic implementation of janitor rule engine that runs all containing rules to decide if a resource should be
 * a candidate of cleanup.
 */
public class BasicJanitorRuleEngine implements JanitorRuleEngine {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicJanitorRuleEngine.class);

    /** The rules to decide if a resource should be a candidate for cleanup. **/
    private final List<Rule> rules;

    /** The rules to decide if a resource should be excluded for cleanup. **/
    private final List<Rule> exclusionRules;

    /**
     * The constructor of JanitorRuleEngine.
     */
    public BasicJanitorRuleEngine() {
        rules = new ArrayList<Rule>();
        exclusionRules = new ArrayList<Rule>();
    }

    /**
     * Decides whether the resource should be a candidate of cleanup based on the underlying rules. If any rule in the
     * rule set thinks the resource should be a candidate of cleanup, the method returns false which indicates that the
     * resource should be marked for cleanup. If multiple rules think the resource should be cleaned up, the rule with
     * the nearest expected termination time fills the termination reason and expected termination time.
     *
     * @param resource
     *            The resource
     * @return true if the resource is valid and should not be a candidate of cleanup based on the underlying rules,
     *         false otherwise.
     */
    @Override
    public boolean isValid(Resource resource) {
        LOGGER.debug(String.format("Checking if resource %s of type %s is a cleanup candidate against %d rules and %d exclusion rules.",
                resource.getId(), resource.getResourceType(), rules.size(), exclusionRules.size()));

        for (Rule exclusionRule : exclusionRules) {
            if (exclusionRule.isValid(resource)) {
                LOGGER.info(String.format("Resource %s is not marked as a cleanup candidate because of an exclusion rule.", resource.getId()));
                return true;
            }
        }

        // We create a clone of the resource each time when we try the rule. In the first iteration of the rules
        // we identify the rule with the nearest termination date if there is any rule considers the resource
        // as a cleanup candidate. Then the rule is applied to the original resource.
        Rule nearestRule = null;
        if (rules.size() == 1) {
            nearestRule = rules.get(0);
        } else {
            Date nearestTerminationTime = null;
            for (Rule rule : rules) {
                Resource clone = resource.cloneResource();
                if (!rule.isValid(clone)) {
                    if (clone.getExpectedTerminationTime() != null) {
                        if (nearestTerminationTime == null || nearestTerminationTime.after(clone.getExpectedTerminationTime())) {
                            nearestRule = rule;
                            nearestTerminationTime = clone.getExpectedTerminationTime();
                        }
                    }
                }
            }
        }
        if (nearestRule != null && !nearestRule.isValid(resource)) {
            LOGGER.info(String.format("Resource %s is marked as a cleanup candidate.", resource.getId()));
            return false;
        } else {
            LOGGER.info(String.format("Resource %s is not marked as a cleanup candidate.", resource.getId()));
            return true;
        }
    }

    /** {@inheritDoc} */
    @Override
    public BasicJanitorRuleEngine addRule(Rule rule) {
        rules.add(rule);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public BasicJanitorRuleEngine addExclusionRule(Rule rule){
        exclusionRules.add(rule);
        return this;
    }

   /** {@inheritDoc} */
    @Override
    public List<Rule> getRules() {
        return this.rules;
    }


    /** {@inheritDoc} */
    @Override
    public List<Rule> getExclusionRules() {
        return this.exclusionRules;
    }

}

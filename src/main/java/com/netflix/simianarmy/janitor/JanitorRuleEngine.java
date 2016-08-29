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

import com.netflix.simianarmy.Resource;
import java.util.List;

/**
 * The interface for janitor rule engine that can decide if a resource should be a candidate of cleanup
 * based on a collection of rules.
 */
public interface JanitorRuleEngine {

    /**
     * Decides whether the resource should be a candidate of cleanup based on the underlying rules.
     *
     * @param resource
     *            The resource
     * @return true if the resource is valid and should not be a candidate of cleanup based on the underlying rules,
     *         false otherwise.
     */
    boolean isValid(Resource resource);

    /**
     * Add a rule to decide if a resource should be a candidate for cleanup.
     *
     * @param rule
     *            The rule to decide if a resource should be a candidate for cleanup.
     * @return The JanitorRuleEngine object.
     */
    JanitorRuleEngine addRule(Rule rule);

    /**
     * Add a rule to decide if a resource should be excluded for cleanup.
     * Exclusion rules are evaluated before regular rules.  If a resource
     * matches an exclusion rule, it is excluded from all other cleanup rules.
     *
     * @param rule
     *            The rule to decide if a resource should be excluded for cleanup.
     * @return The JanitorRuleEngine object.
     */
    JanitorRuleEngine addExclusionRule(Rule rule);

    /**
     * Get rules to find out what's planned for enforcement.
     *
     * @return An ArrayList of Rules.
     */
    List<Rule> getRules();

    /**
     * Get rules to find out what's excluded for enforcement.
     *
     * @return An ArrayList of Rules.
     */
    List<Rule> getExclusionRules();
}

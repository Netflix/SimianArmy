/*
 *
 *  Copyright 2013 Netflix, Inc.
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
package com.netflix.simianarmy.conformity;

import com.google.common.collect.Lists;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * The class implementing the conformity rule engine.
 */
public class ConformityRuleEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConformityRuleEngine.class);

    private final Collection<ConformityRule> rules = Lists.newArrayList();

    /**
     * Checks whether a cluster is conforming or not against the rules in the engine. This
     * method runs the checks the cluster against all the rules.
     *
     * @param cluster
     *            the cluster
     * @return true if the cluster is conforming, false otherwise.
     */
    public boolean check(Cluster cluster) {
        Validate.notNull(cluster);
        cluster.clearConformities();
        for (ConformityRule rule : rules) {
            if (!cluster.getExcludedRules().contains(rule.getName())) {
                LOGGER.info(String.format("Running conformity rule %s on cluster %s",
                        rule.getName(), cluster.getName()));
                cluster.updateConformity(rule.check(cluster));
            } else {
                LOGGER.info(String.format("Conformity rule %s is excluded on cluster %s",
                        rule.getName(), cluster.getName()));
            }
        }
        boolean isConforming = true;
        for (Conformity conformity : cluster.getConformties()) {
            if (!conformity.getFailedComponents().isEmpty()) {
                isConforming = false;
            }
        }
        cluster.setConforming(isConforming);
        return isConforming;
    }

    /**
     * Add a conformity rule.
     *
     * @param rule
     *            The conformity rule to add.
     * @return The Conformity rule engine object.
     */
    public ConformityRuleEngine addRule(ConformityRule rule) {
        Validate.notNull(rule);
        rules.add(rule);
        return this;
    }

    /**
     * Gets all conformity rules in the rule engine.
     * @return all conformity rules in the rule engine
     */
    public Collection<ConformityRule> rules() {
        return Collections.unmodifiableCollection(rules);
    }
}

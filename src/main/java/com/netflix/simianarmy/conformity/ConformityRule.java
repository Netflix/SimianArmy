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

/**
 * Interface for a conformity check rule.
 */
public interface ConformityRule {

    /**
     * Performs the conformity check against the rule.
     * @param cluster
     *          the cluster to check for conformity
     * @return
     *          the conformity check result
     */
    Conformity check(Cluster cluster);

    /**
     * Gets the name/id of the rule.
     * @return
     *      the name of the rule
     */
    String getName();

    /**
     * Gets the human-readable reason to explain why the cluster is not conforming.
     * @return the human-readable reason to explain why the cluster is not conforming
     */
    String getNonconformingReason();
}

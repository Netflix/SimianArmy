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

import java.util.Collection;
import java.util.Collections;

/**
 * The class defining the result of a conformity check.
 */
public class Conformity {

    private final String ruleId;
    private final Collection<String> failedComponents = Lists.newArrayList();

    /**
     * Constructor.
     * @param ruleId
     *          the conformity rule id
     * @param failedComponents
     *          the components that cause the conformity check to fail, if there is
     *          no failed components, it means the conformity check passes.
     */
    public Conformity(String ruleId, Collection<String> failedComponents) {
        Validate.notNull(ruleId);
        Validate.notNull(failedComponents);
        this.ruleId = ruleId;
        for (String failedComponent : failedComponents) {
            this.failedComponents.add(failedComponent);
        }
    }

    /**
     * Gets the conformity rule id.
     * @return
     *      the conformity rule id
     */
    public String getRuleId() {
        return ruleId;
    }

    /**
     * Gets the components that cause the conformity check to fail.
     * @return
     *      the components that cause the conformity check to fail
     */
    public Collection<String> getFailedComponents() {
        return Collections.unmodifiableCollection(failedComponents);
    }
}

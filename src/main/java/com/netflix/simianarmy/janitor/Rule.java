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

/**
 * The rule implementing a logic to decide if a resource should be considered as a candidate of cleanup.
 */
public interface Rule {
    /**
     * Decides whether the resource should be a candidate of cleanup based on the underlying rule. When
     * the rule considers the resource as a candidate of cleanup, it sets the expected termination time
     * and termination reason of the resource.
     *
     * @param resource
     *            The resource
     * @return true if the resource is valid and is not for cleanup, false otherwise
     */
    boolean isValid(Resource resource);
}

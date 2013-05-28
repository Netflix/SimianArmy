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

import com.netflix.simianarmy.AbstractEmailBuilder;

import java.util.Collection;
import java.util.Map;

/** The abstract class for building Conformity monkey email notifications. */
public abstract class ConformityEmailBuilder extends AbstractEmailBuilder {

    /**
     * Sets the map from an owner email to the clusters that belong to the owner
     * and need to send notifications for.
     * @param emailToClusters the map from owner email to the owned clusters
     * @param rules all conformity rules that are used to find the description of each rule to display
     */
    public abstract void setEmailToClusters(Map<String, Collection<Cluster>> emailToClusters,
            Collection<ConformityRule> rules);
}

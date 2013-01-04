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

import java.util.Collection;
import java.util.Map;

import com.netflix.simianarmy.AbstractEmailBuilder;
import com.netflix.simianarmy.Resource;

/** The abstract class for building Janitor monkey email notifications. */
public abstract class JanitorEmailBuilder extends AbstractEmailBuilder {

    /**
     * Sets the map from an owner email to the resources that belong to the owner
     * and need to send notifications for.
     * @param emailToResources the map from owner email to the owned resource
     */
    public abstract void setEmailToResources(Map<String, Collection<Resource>> emailToResources);
}

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

import com.netflix.simianarmy.ResourceType;

/**
 * The interface for a janitor that performs the mark and cleanup operations for
 * cloud resources of a resource type.
 */
public interface Janitor {

    /**
     * Gets the resource type the janitor is cleaning up.
     * @return the resource type the janitor is cleaning up.
     */
    ResourceType getResourceType();

    /**
     * Mark cloud resources as cleanup candidates and remove the marks for resources
     * that no longer exist or should not be cleanup candidates anymore.
     */
    void markResources();

    /**
     * Clean the resources up that are marked as cleanup candidates when appropriate.
     */
    void cleanupResources();
}

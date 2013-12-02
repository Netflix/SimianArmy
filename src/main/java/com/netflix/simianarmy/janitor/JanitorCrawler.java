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

import java.util.EnumSet;
import java.util.List;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;

/**
 * The crawler for janitor monkey.
 */
public interface JanitorCrawler {

    /**
     * Resource types.
     *
     * @return the type of resources this crawler crawls
     */
    EnumSet<? extends ResourceType> resourceTypes();

    /**
     * Resources crawled by this crawler for a specific resource type.
     *
     * @param resourceType the resource type
     * @return the list
     */
    List<Resource> resources(ResourceType resourceType);

    /**
     * Gets the up to date information for a collection of resource ids. When the input argument is null
     * or empty, the method returns all resources.
     *
     * @param resourceIds
     *          the resource ids
     * @return the list of resources
     */
    List<Resource> resources(String... resourceIds);

    /**
     * Gets the owner email for a resource to set the ownerEmail field when crawl.
     * @param resource the resource
     * @return the owner email of the resource
     */
    String getOwnerEmailForResource(Resource resource);
}

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
import com.netflix.simianarmy.ResourceType;

import java.util.List;

/**
 * The interface to track the resources marked/cleaned by the Janitor Monkey.
 *
 */
public interface JanitorResourceTracker {

    /**
     * Adds a resource to the tracker. If the resource with the same id already exists
     * in the tracker, the method updates the record with the resource parameter.
     * @param resource the resource to add or update
     */
    void addOrUpdate(Resource resource);

    /** Gets the list of resources of a specific resource type and cleanup state in a region.
     *
     * @param resourceType the resource type
     * @param state the cleanup state of the resources
     * @param region the region of the resources, when the parameter is null, the method returns
     * resources from all regions
     * @return list of resources that match the resource type, state and region
     */
    List<Resource> getResources(ResourceType resourceType, Resource.CleanupState state, String region);

    /** Gets the resource of a specific id.
     *
     * @param resourceId the resource id
     * @return the resource that matches the resource id
     */
    Resource getResource(String resourceId);

    /** Gets the resource of a specific id.
     *
     * @param resourceId the resource id
     * @param regionId the region id
     * @return the resource that matches the resource id and region
     */
    Resource getResource(String resourceId, String regionId);

}

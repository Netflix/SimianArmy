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

package com.netflix.simianarmy;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

/**
 * The interface of Resource. It defines the interfaces for getting the common properties of a resource, as well as
 * the methods to add and retrieve the additional properties of a resource. Instead of defining a new subclass of
 * the Resource interface, new resources that have additional fields other than the common ones can be represented,
 * by adding field-value pairs. This approach makes serialization and deserialization of resources much easier with
 * the cost of type safety.
 */
public interface Resource {
    /** The enum representing the cleanup state of a resource. **/
    public enum CleanupState {
        /** The resource is marked as a cleanup candidate but has not been cleaned up yet. **/
        MARKED,
        /** The resource is terminated by janitor monkey. **/
        JANITOR_TERMINATED,
        /** The resource is terminated by user before janitor monkey performs the termination. **/
        USER_TERMINATED,
        /** The resource is unmarked and not for cleanup anymore due to some change of situations. **/
        UNMARKED
    }

    /**
     * Gets the resource id.
     *
     * @return the resource id
     */
    String getId();

    /**
     * Sets the resource id.
     *
     * @param id the resource id
     */
    void setId(String id);

    /**
     * Sets the resource id and returns the resource.
     *
     * @param id the resource id
     * @return the resource object
     */
    Resource withId(String id);

    /**
     * Gets the resource type.
     *
     * @return the resource type enum
     */
    ResourceType getResourceType();

    /**
     * Sets the resource type.
     *
     * @param type the resource type enum
     */
    void setResourceType(ResourceType type);

    /**
     * Sets the resource type and returns the resource.
     *
     * @param type resource type enum
     * @return the resource object
     */
    Resource withResourceType(ResourceType type);

    /**
     * Gets the region the resource is in.
     *
     * @return the region of the resource
     */
    String getRegion();

    /**
     * Sets the region the resource is in.
     *
     * @param region the region the resource is in
     */
    void setRegion(String region);

    /**
     * Sets the resource region and returns the resource.
     *
     * @param region the region the resource is in
     * @return the resource object
     */
    Resource withRegion(String region);

    /**
     * Gets the owner email of the resource.
     *
     * @return the owner email of the resource
     */
    String getOwnerEmail();

    /**
     * Sets the owner email of the resource.
     *
     * @param ownerEmail the owner email of the resource
     */
    void setOwnerEmail(String ownerEmail);

    /**
     * Sets the resource owner email and returns the resource.
     *
     * @param ownerEmail the owner email of the resource
     * @return the resource object
     */
    Resource withOwnerEmail(String ownerEmail);

    /**
     * Gets the description of the resource.
     *
     * @return the description of the resource
     */
    String getDescription();

    /**
     * Sets the description of the resource.
     *
     * @param description the description of the resource
     */
    void setDescription(String description);

    /**
     * Sets the resource description and returns the resource.
     *
     * @param description the description of the resource
     * @return the resource object
     */
    Resource withDescription(String description);

    /**
     * Gets the launch time of the resource.
     *
     * @return the launch time of the resource
     */
    Date getLaunchTime();

    /**
     * Sets the launch time of the resource.
     *
     * @param launchTime the launch time of the resource
     */
    void setLaunchTime(Date launchTime);

    /**
     * Sets the resource launch time and returns the resource.
     *
     * @param launchTime the launch time of the resource
     * @return the resource object
     */
    Resource withLaunchTime(Date launchTime);

    /**
     * Gets the time that when the resource is marked as a cleanup candidate.
     *
     * @return the time that when the resource is marked as a cleanup candidate
     */
    Date getMarkTime();

    /**
     * Sets the time that when the resource is marked as a cleanup candidate.
     *
     * @param markTime the time that when the resource is marked as a cleanup candidate
     */
    void setMarkTime(Date markTime);

    /**
     * Sets the resource mark time and returns the resource.
     *
     * @param markTime the time that when the resource is marked as a cleanup candidate
     * @return the resource object
     */
    Resource withMarkTime(Date markTime);

    /**
     * Gets the the time that when the resource is expected to be terminated.
     *
     * @return the time that when the resource is expected to be terminated
     */
    Date getExpectedTerminationTime();

    /**
     * Sets the time that when the resource is expected to be terminated.
     *
     * @param expectedTerminationTime the time that when the resource is expected to be terminated
     */
    void setExpectedTerminationTime(Date expectedTerminationTime);

    /**
     * Sets the time that when the resource is expected to be terminated and returns the resource.
     *
     * @param expectedTerminationTime the time that when the resource is expected to be terminated
     * @return the resource object
     */
    Resource withExpectedTerminationTime(Date expectedTerminationTime);

    /**
     * Gets the time that when the resource is actually terminated.
     *
     * @return the time that when the resource is actually terminated
     */
    Date getActualTerminationTime();

    /**
     * Sets the time that when the resource is actually terminated.
     *
     * @param actualTerminationTime the time that when the resource is actually terminated
     */
    void setActualTerminationTime(Date actualTerminationTime);

    /**
     * Sets the resource actual termination time and returns the resource.
     *
     * @param actualTerminationTime the time that when the resource is actually terminated
     * @return the resource object
     */
    Resource withActualTerminationTime(Date actualTerminationTime);

    /**
     * Gets the time that when the owner is notified about the cleanup of the resource.
     *
     * @return the time that when the owner is notified about the cleanup of the resource
     */
    Date getNotificationTime();

    /**
     * Sets the time that when the owner is notified about the cleanup of the resource.
     *
     * @param notificationTime the time that when the owner is notified about the cleanup of the resource
     */
    void setNotificationTime(Date notificationTime);

    /**
     * Sets the time that when the owner is notified about the cleanup of the resource and returns the resource.
     *
     * @param notificationTime the time that when the owner is notified about the cleanup of the resource
     * @return the resource object
     */
    Resource withNnotificationTime(Date notificationTime);

    /**
     * Gets the resource state.
     *
     * @return the resource state enum
     */
    CleanupState getState();

    /**
     * Sets the resource state.
     *
     * @param state the resource state
     */
    void setState(CleanupState state);

    /**
     * Sets the resource state and returns the resource.
     *
     * @param state resource state enum
     * @return the resource object
     */
    Resource withState(CleanupState state);

    /**
     * Gets the termination reason of the resource.
     *
     * @return the termination reason of the resource
     */
    String getTerminationReason();

    /**
     * Sets the termination reason of the resource.
     *
     * @param terminationReason the termination reason of the resource
     */
    void setTerminationReason(String terminationReason);

    /**
     * Sets the resource termination reason and returns the resource.
     *
     * @param terminationReason the termination reason of the resource
     * @return the resource object
     */
    Resource withTerminationReason(String terminationReason);

    /**
     * Gets the boolean to indicate whether or not the resource is opted out of Janitor monkey
     * so it will not be cleaned.
     * @return true if the resource is opted out of Janitor monkey, otherwise false
     */
    boolean isOptOutOfJanitor();

    /**
     * Sets the flag to indicate whether or not the resource is opted out of Janitor monkey
     * so it will not be cleaned.
     * @param optOutOfJanitor true if the resource is opted out of Janitor monkey, otherwise false
     */
    void setOptOutOfJanitor(boolean optOutOfJanitor);

    /**
     * Sets the flag to indicate whether or not the resource is opted out of Janitor monkey
     * so it will not be cleaned and returns the resource object.
     * @param optOutOfJanitor true if the resource is opted out of Janitor monkey, otherwise false
     * @return the resource object
     */
    Resource withOptOutOfJanitor(boolean optOutOfJanitor);

    /**
     * Gets a map from fields of resources to corresponding values. Values are represented
     * as Strings so they can be displayed or stored in databases like SimpleDB.
     * @return a map from field name to field value
     */
    Map<String, String> getFieldToValueMap();

    /** Adds or sets an additional field with the specified name and value to the resource.
     *
     * @param fieldName the field name
     * @param fieldValue the field value
     * @return the resource itself for chaining
     */
    Resource setAdditionalField(String fieldName, String fieldValue);

    /** Gets the value of an additional field with the specified name of the resource.
     *
     * @param fieldName the field name
     * @return the field value
     */
    String getAdditionalField(String fieldName);

    /**
     * Gets all additional field names in the resource.
     * @return a collection of names of all additional fields
     */
    Collection<String> getAdditionalFieldNames();

    /**
     * Adds a tag with the specified key and value to the resource.
     * @param key the key of the tag
     * @param value the value of the tag
     */
    void setTag(String key, String value);

    /**
     * Gets the tag value for a specific key of the resource.
     * @param key the key of the tag
     * @return the value of the tag
     */
    String getTag(String key);

    /**
     * Gets all the keys of tags.
     * @return collection of keys of all tags
     */
    Collection<String> getAllTagKeys();


    /** Clone a resource with the exact field values of the current object.
     *
     * @return the clone of the resource
     */
    Resource cloneResource();
}

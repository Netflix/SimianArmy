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

package com.netflix.simianarmy.aws;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.netflix.simianarmy.NamedType;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;

/**
 * The class represents general AWS resources that are managed by janitor monkey.
 */
public class AWSResource implements Resource {
    private String id;
    private ResourceType resourceType;
    private String region;
    private String ownerEmail;
    private String description;
    private String terminationReason;
    private CleanupState state;
    private Date expectedTerminationTime;
    private Date actualTerminationTime;
    private Date notificationTime;
    private Date launchTime;
    private Date markTime;
    private boolean optOutOfJanitor;
    private String awsResourceState;

    /** The field name for resourceId. **/
    public static final String FIELD_RESOURCE_ID = "resourceId";
    /** The field name for resourceType. **/
    public static final String FIELD_RESOURCE_TYPE = "resourceType";
    /** The field name for region. **/
    public static final String FIELD_REGION = "region";
    /** The field name for owner email. **/
    public static final String FIELD_OWNER_EMAIL = "ownerEmail";
    /** The field name for description. **/
    public static final String FIELD_DESCRIPTION = "description";
    /** The field name for state. **/
    public static final String FIELD_STATE = "state";
    /** The field name for terminationReason. **/
    public static final String FIELD_TERMINATION_REASON = "terminationReason";
    /** The field name for expectedTerminationTime. **/
    public static final String FIELD_EXPECTED_TERMINATION_TIME = "expectedTerminationTime";
    /** The field name for actualTerminationTime. **/
    public static final String FIELD_ACTUAL_TERMINATION_TIME = "actualTerminationTime";
    /** The field name for notificationTime. **/
    public static final String FIELD_NOTIFICATION_TIME = "notificationTime";
    /** The field name for launchTime. **/
    public static final String FIELD_LAUNCH_TIME = "launchTime";
    /** The field name for markTime. **/
    public static final String FIELD_MARK_TIME = "markTime";
    /** The field name for isOptOutOfJanitor. **/
    public static final String FIELD_OPT_OUT_OF_JANITOR = "optOutOfJanitor";
    /** The field name for awsResourceState. **/
    public static final String FIELD_AWS_RESOURCE_STATE = "awsResourceState";

    /** The date format used to print or parse a Date value. **/
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

    /** The map from name to value for additional fields used by the resource. **/
    private final Map<String, String> additionalFields = new HashMap<String, String>();

    /** The map from AWS tag key to value for the resource. **/
    private final Map<String, String> tags = new HashMap<String, String>();

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getFieldToValueMap() {
        Map<String, String> fieldToValue = new HashMap<String, String>();

        putToMapIfNotNull(fieldToValue, FIELD_RESOURCE_ID, getId());
        putToMapIfNotNull(fieldToValue, FIELD_RESOURCE_TYPE, getResourceType());
        putToMapIfNotNull(fieldToValue, FIELD_REGION, getRegion());
        putToMapIfNotNull(fieldToValue, FIELD_OWNER_EMAIL, getOwnerEmail());
        putToMapIfNotNull(fieldToValue, FIELD_DESCRIPTION, getDescription());
        putToMapIfNotNull(fieldToValue, FIELD_STATE, getState());
        putToMapIfNotNull(fieldToValue, FIELD_TERMINATION_REASON, getTerminationReason());
        putToMapIfNotNull(fieldToValue, FIELD_EXPECTED_TERMINATION_TIME, printDate(getExpectedTerminationTime()));
        putToMapIfNotNull(fieldToValue, FIELD_ACTUAL_TERMINATION_TIME, printDate(getActualTerminationTime()));
        putToMapIfNotNull(fieldToValue, FIELD_NOTIFICATION_TIME, printDate(getNotificationTime()));
        putToMapIfNotNull(fieldToValue, FIELD_LAUNCH_TIME, printDate(getLaunchTime()));
        putToMapIfNotNull(fieldToValue, FIELD_MARK_TIME, printDate(getMarkTime()));
        putToMapIfNotNull(fieldToValue, FIELD_AWS_RESOURCE_STATE, getAWSResourceState());

        // Additional fields are serialized while tags are not. So if any tags need to be
        // serialized as well, put them to additional fields.
        fieldToValue.put(FIELD_OPT_OUT_OF_JANITOR, String.valueOf(isOptOutOfJanitor()));

        fieldToValue.putAll(additionalFields);

        return fieldToValue;
    }

    /**
     * Parse a map from field name to value to a resource.
     * @param fieldToValue the map from field name to value
     * @return the resource that is de-serialized from the map
     */
    public static AWSResource parseFieldtoValueMap(Map<String, String> fieldToValue) {
        AWSResource resource = new AWSResource();
        for (Map.Entry<String, String> field : fieldToValue.entrySet()) {
            String name = field.getKey();
            String value = field.getValue();
            if (name.equals(FIELD_RESOURCE_ID)) {
                resource.setId(value);
            } else if (name.equals(FIELD_RESOURCE_TYPE)) {
                resource.setResourceType(AWSResourceType.valueOf(value));
            } else if (name.equals(FIELD_REGION)) {
                resource.setRegion(value);
            } else if (name.equals(FIELD_OWNER_EMAIL)) {
                resource.setOwnerEmail(value);
            } else if (name.equals(FIELD_DESCRIPTION)) {
                resource.setDescription(value);
            } else if (name.equals(FIELD_STATE)) {
                resource.setState(CleanupState.valueOf(value));
            } else if (name.equals(FIELD_TERMINATION_REASON)) {
                resource.setTerminationReason(value);
            } else if (name.equals(FIELD_EXPECTED_TERMINATION_TIME)) {
                resource.setExpectedTerminationTime(new Date(DATE_FORMATTER.parseDateTime(value).getMillis()));
            } else if (name.equals(FIELD_ACTUAL_TERMINATION_TIME)) {
                resource.setActualTerminationTime(new Date(DATE_FORMATTER.parseDateTime(value).getMillis()));
            } else if (name.equals(FIELD_NOTIFICATION_TIME)) {
                resource.setNotificationTime(new Date(DATE_FORMATTER.parseDateTime(value).getMillis()));
            } else if (name.equals(FIELD_LAUNCH_TIME)) {
                resource.setLaunchTime(new Date(DATE_FORMATTER.parseDateTime(value).getMillis()));
            } else if (name.equals(FIELD_MARK_TIME)) {
                resource.setMarkTime(new Date(DATE_FORMATTER.parseDateTime(value).getMillis()));
            } else if (name.equals(FIELD_AWS_RESOURCE_STATE)) {
                resource.setAWSResourceState(value);
            } else if (name.equals(FIELD_OPT_OUT_OF_JANITOR)) {
                resource.setOptOutOfJanitor("true".equals(value));
            } else {
                // put all other fields into additional fields
                resource.setAdditionalField(name, value);
            }
        }
        return resource;
    }

    public String getAWSResourceState() {
        return awsResourceState;
    }

    public void setAWSResourceState(String awsState) {
        this.awsResourceState = awsState;
    }

    /** {@inheritDoc} */
    @Override
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public void setId(String id) {
        this.id = id;
    }

    /** {@inheritDoc} */
    @Override
    public Resource withId(String resourceId) {
        setId(resourceId);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ResourceType getResourceType() {
        return resourceType;
    }

    /** {@inheritDoc} */
    @Override
    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /** {@inheritDoc} */
    @Override
    public Resource withResourceType(ResourceType type) {
        setResourceType(type);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getRegion() {
        return region;
    }

    /** {@inheritDoc} */
    @Override
    public void setRegion(String region) {
        this.region = region;
    }

    /** {@inheritDoc} */
    @Override
    public Resource withRegion(String resourceRegion) {
        setRegion(resourceRegion);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getOwnerEmail() {
        return ownerEmail;
    }

    /** {@inheritDoc} */
    @Override
    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    /** {@inheritDoc} */
    @Override
    public Resource withOwnerEmail(String resourceOwner) {
        setOwnerEmail(resourceOwner);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return description;
    }

    /** {@inheritDoc} */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /** {@inheritDoc} */
    @Override
    public Resource withDescription(String resourceDescription) {
        setDescription(resourceDescription);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Date getLaunchTime() {
        return getCopyOfDate(launchTime);
    }

    /** {@inheritDoc} */
    @Override
    public void setLaunchTime(Date launchTime) {
        this.launchTime = getCopyOfDate(launchTime);
    }

    /** {@inheritDoc} */
    @Override
    public Resource withLaunchTime(Date resourceLaunchTime) {
        setLaunchTime(resourceLaunchTime);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Date getMarkTime() {
        return getCopyOfDate(markTime);
    }

    /** {@inheritDoc} */
    @Override
    public void setMarkTime(Date markTime) {
        this.markTime = getCopyOfDate(markTime);
    }

    /** {@inheritDoc} */
    @Override
    public Resource withMarkTime(Date resourceMarkTime) {
        setMarkTime(resourceMarkTime);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Date getExpectedTerminationTime() {
        return getCopyOfDate(expectedTerminationTime);
    }

    /** {@inheritDoc} */
    @Override
    public void setExpectedTerminationTime(Date expectedTerminationTime) {
        this.expectedTerminationTime = getCopyOfDate(expectedTerminationTime);
    }

    /** {@inheritDoc} */
    @Override
    public Resource withExpectedTerminationTime(Date resourceExpectedTerminationTime) {
        setExpectedTerminationTime(resourceExpectedTerminationTime);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Date getActualTerminationTime() {
        return getCopyOfDate(actualTerminationTime);
    }

    /** {@inheritDoc} */
    @Override
    public void setActualTerminationTime(Date actualTerminationTime) {
        this.actualTerminationTime = getCopyOfDate(actualTerminationTime);
    }

    /** {@inheritDoc} */
    @Override
    public Resource withActualTerminationTime(Date resourceActualTerminationTime) {
        setActualTerminationTime(resourceActualTerminationTime);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public Date getNotificationTime() {
        return getCopyOfDate(notificationTime);
    }

    /** {@inheritDoc} */
    @Override
    public void setNotificationTime(Date notificationTime) {
        this.notificationTime = getCopyOfDate(notificationTime);
    }

    /** {@inheritDoc} */
    @Override
    public Resource withNnotificationTime(Date resourceNotificationTime) {
        setNotificationTime(resourceNotificationTime);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public CleanupState getState() {
        return state;
    }

    /** {@inheritDoc} */
    @Override
    public void setState(CleanupState state) {
        this.state = state;
    }

    /** {@inheritDoc} */
    @Override
    public Resource withState(CleanupState resourceState) {
        setState(resourceState);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getTerminationReason() {
        return terminationReason;
    }

    /** {@inheritDoc} */
    @Override
    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    /** {@inheritDoc} */
    @Override
    public Resource withTerminationReason(String resourceTerminationReason) {
        setTerminationReason(resourceTerminationReason);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isOptOutOfJanitor() {
        return optOutOfJanitor;
    }

    /** {@inheritDoc} */
    @Override
    public void setOptOutOfJanitor(boolean optOutOfJanitor) {
        this.optOutOfJanitor = optOutOfJanitor;
    }

    /** {@inheritDoc} */
    @Override
    public Resource withOptOutOfJanitor(boolean optOut) {
        setOptOutOfJanitor(optOut);
        return this;
    }

    private static Date getCopyOfDate(Date date) {
        if (date == null) {
            return null;
        }
        return new Date(date.getTime());
    }

    private static void putToMapIfNotNull(Map<String, String> map, String key, String value) {
        Validate.notNull(map);
        Validate.notNull(key);
        if (value != null) {
            map.put(key, value);
        }
    }

    private static void putToMapIfNotNull(Map<String, String> map, String key, Enum<?> value) {
        Validate.notNull(map);
        Validate.notNull(key);
        if (value != null) {
            map.put(key, value.name());
        }
    }

    private static void putToMapIfNotNull(Map<String, String> map, String key, NamedType value) {
        Validate.notNull(map);
        Validate.notNull(key);
        if (value != null) {
            map.put(key, value.name());
        }
    }

    private static String printDate(Date date) {
        if (date == null) {
            return null;
        }

        return DATE_FORMATTER.print(date.getTime());
    }

    @Override
    public Resource setAdditionalField(String fieldName, String fieldValue) {
        Validate.notNull(fieldName);
        Validate.notNull(fieldValue);
        putToMapIfNotNull(additionalFields, fieldName, fieldValue);
        return this;
    }

    @Override
    public String getAdditionalField(String fieldName) {
        return additionalFields.get(fieldName);
    }

    @Override
    public Collection<String> getAdditionalFieldNames() {
        return additionalFields.keySet();
    }

    @Override
    public Resource cloneResource() {
        Resource clone = new AWSResource()
        .withActualTerminationTime(getActualTerminationTime())
        .withDescription(getDescription())
        .withExpectedTerminationTime(getExpectedTerminationTime())
        .withId(getId())
        .withLaunchTime(getLaunchTime())
        .withMarkTime(getMarkTime())
        .withNnotificationTime(getNotificationTime())
        .withOwnerEmail(getOwnerEmail())
        .withRegion(getRegion())
        .withResourceType(getResourceType())
        .withState(getState())
        .withTerminationReason(getTerminationReason())
        .withOptOutOfJanitor(isOptOutOfJanitor());
        ((AWSResource) clone).setAWSResourceState(awsResourceState);

        ((AWSResource) clone).additionalFields.putAll(additionalFields);

        for (String key : this.getAllTagKeys()) {
            clone.setTag(key, this.getTag(key));
        }

        return clone;
    }

    /** {@inheritDoc} */
    @Override
    public void setTag(String key, String value) {
        tags.put(key, value);
    }

    /** {@inheritDoc} */
    @Override
    public String getTag(String key) {
        return tags.get(key);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getAllTagKeys() {
        return tags.keySet();
    }

}

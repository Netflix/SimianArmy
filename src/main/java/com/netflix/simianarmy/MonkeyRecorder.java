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

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The Interface MonkeyRecorder. This is use to store and find events in some datastore.
 */
public interface MonkeyRecorder {

    /**
     * The Interface Event.
     */
    public interface Event {

        /**
         * Event Id.
         *
         * @return the string
         */
        String id();

        /**
         * Event time.
         *
         * @return the date
         */
        Date eventTime();

        /**
         * Monkey type.
         *
         * @return the monkey type enum
         */
        MonkeyType monkeyType();

        /**
         * Event type.
         *
         * @return the event type enum
         */
        EventType eventType();

        /**
         * Region.
         *
         * @return the region for the event
         */
        String region();

        /**
         * Fields.
         *
         *
         * @return the map of strings that may have been provided when the event was created
         */
        Map<String, String> fields();

        /**
         * Field.
         *
         * @param name
         *            the name
         * @return the string associated with that field
         */
        String field(String name);

        /**
         * Adds the field.
         *
         * @param name
         *            the name
         * @param value
         *            the value
         * @return <b>this</b> so you can chain multiple addField calls together
         */
        Event addField(String name, String value);
    }

    /**
     * New event.
     *
     * @param monkeyType
     *            the monkey type
     * @param eventType
     *            the event type
     * @param region
     *            the region the event occurred
     * @param id
     *            the id
     * @return the event
     */
    Event newEvent(MonkeyType monkeyType, EventType eventType, String region, String id);

    default Event newEvent(MonkeyType monkeyType, EventType eventType, Resource resource, String id) {
        if (resource == null) throw new IllegalArgumentException("resource must not be null");
        Event event = newEvent(monkeyType, eventType, resource.getRegion(), id);
        if (resource.getAllTagKeys() != null) {
            for(String key : resource.getAllTagKeys()) {
                event.addField(key, resource.getTag(key));
            }
        }
        event.addField("ResourceDescription", resource.getDescription());
        event.addField("ResourceType", resource.getResourceType().toString());
        event.addField("ResourceId", resource.getId());
        return event;
    }

    /**
     * Record event.
     *
     * @param evt
     *            the evt
     */
    void recordEvent(Event evt);

    /**
     * Find events.
     *
     * @param query
     *            arbitrary map of strings to used to filter the results
     * @param after
     *            the after
     * @return the list of events
     */
    List<Event> findEvents(Map<String, String> query, Date after);

    /**
     * Find events.
     *
     * @param monkeyType
     *            the monkey type
     * @param query
     *            arbitrary map of strings to used to filter the results
     * @param after
     *            the after
     * @return the list of events
     */
    List<Event> findEvents(MonkeyType monkeyType, Map<String, String> query, Date after);

    /**
     * Find events.
     *
     * @param monkeyType
     *            the monkey type
     * @param eventType
     *            the event type
     * @param query
     *            arbitrary map of strings to used to filter the results
     * @param after
     *            the after
     * @return the list
     */
    List<Event> findEvents(MonkeyType monkeyType, EventType eventType, Map<String, String> query, Date after);
}

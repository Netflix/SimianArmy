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
package com.netflix.simianarmy.basic;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyType;

/**
 * The Class BasicRecorderEvent.
 */
public class BasicRecorderEvent implements MonkeyRecorder.Event {

    /** The monkey type. */
    private MonkeyType monkeyType;

    /** The event type. */
    private EventType eventType;

    /** The event id. */
    private String id;

    /** The event region. */
    private String region;

    /** The fields. */
    private Map<String, String> fields = new HashMap<String, String>();

    /** The event time. */
    private Date date;

    /**
     * Instantiates a new basic recorder event.
     *
     * @param monkeyType
     *            the monkey type
     * @param eventType
     *            the event type
     * @param region
     *            the region event occurred in
     * @param id
     *            the event id
     */
    public BasicRecorderEvent(MonkeyType monkeyType, EventType eventType, String region, String id) {
        this.monkeyType = monkeyType;
        this.eventType = eventType;
        this.id = id;
        this.region = region;
        this.date = new Date();
    }

    /**
     * Instantiates a new basic recorder event.
     *
     * @param monkeyType
     *            the monkey type
     * @param eventType
     *            the event type
     * @param region
     *            the region event occurred in
     * @param id
     *            the event id
     * @param time
     *            the event time
     */
    public BasicRecorderEvent(MonkeyType monkeyType, EventType eventType, String region, String id, long time) {
        this.monkeyType = monkeyType;
        this.eventType = eventType;
        this.id = id;
        this.region = region;
        this.date = new Date(time);
    }

    /** {@inheritDoc} */
    public String id() {
        return id;
    }

    /** {@inheritDoc} */
    public String region() {
        return region;
    }

    /** {@inheritDoc} */
    public Date eventTime() {
        return new Date(date.getTime());
    }

    /** {@inheritDoc} */
    public MonkeyType monkeyType() {
        return monkeyType;
    }

    /** {@inheritDoc} */
    public EventType eventType() {
        return eventType;
    }

    /** {@inheritDoc} */
    public Map<String, String> fields() {
        return Collections.unmodifiableMap(fields);
    }

    /** {@inheritDoc} */
    public String field(String name) {
        return fields.get(name);
    }

    /**
     * Adds the fields.
     *
     * @param toAdd
     *            the fields to set
     * @return <b>this</b> so you can chain many addFields calls together
     */
    public MonkeyRecorder.Event addFields(Map<String, String> toAdd) {
        fields.putAll(toAdd);
        return this;
    }

    /** {@inheritDoc} */
    public MonkeyRecorder.Event addField(String name, String value) {
        fields.put(name, value);
        return this;
    }
}

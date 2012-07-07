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

import com.netflix.simianarmy.MonkeyRecorder;

import java.util.Map;
import java.util.HashMap;
import java.util.Date;
import java.util.Collections;

public class BasicRecorderEvent implements MonkeyRecorder.Event {
    private Enum monkeyType;
    private Enum eventType;
    private String id;
    private Map<String, String> fields = new HashMap<String, String>();
    private Date date;

    public BasicRecorderEvent(Enum monkeyType, Enum eventType, String id) {
        this.monkeyType = monkeyType;
        this.eventType = eventType;
        this.id = id;
        this.date = new Date();
    }

    public BasicRecorderEvent(Enum monkeyType, Enum eventType, String id, long time) {
        this.monkeyType = monkeyType;
        this.eventType = eventType;
        this.id = id;
        this.date = new Date(time);
    }

    public String id() {
        return id;
    }

    public Date eventTime() {
        return new Date(date.getTime());
    }

    public Enum monkeyType() {
        return monkeyType;
    }

    public Enum eventType() {
        return eventType;
    }

    public Map<String, String> fields() {
        return Collections.unmodifiableMap(fields);
    }

    public String field(String name) {
        return fields.get(name);
    }

    public MonkeyRecorder.Event addFields(Map<String, String> input) {
        fields.putAll(input);
        return this;
    }

    public MonkeyRecorder.Event addField(String name, String value) {
        fields.put(name, value);
        return this;
    }
}

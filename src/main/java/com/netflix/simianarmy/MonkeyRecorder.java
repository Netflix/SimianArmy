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

import java.util.Map;
import java.util.List;
import java.util.Date;

public interface MonkeyRecorder {

    public interface Event {
        String id();

        Date eventTime();

        Enum monkeyType();

        Enum eventType();

        Map<String, String> fields();

        String field(String name);

        Event addField(String name, String value);
    }

    Event newEvent(Enum monkeyType, Enum eventType, String id);

    void recordEvent(Event evt);

    List<Event> findEvent(Enum monkeyType, Enum eventType);

    List<Event> findEvent(Enum monkeyType, Enum eventType, String id);

    List<Event> findEvent(Enum monkeyType, Enum eventType, Date after);
}

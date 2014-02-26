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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentNavigableMap;

import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;

import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyType;
import com.netflix.simianarmy.chaos.ChaosMonkey;

/**
 * Replacement for SimpleDB on non-AWS: use an embedded db.
 *
 * @author jgardner
 *
 */
public class LocalDbRecorder implements MonkeyRecorder {

    private static DB db = null;
    private static Atomic.Long nextId = null;
    private static ConcurrentNavigableMap<Fun.Tuple2<Long, Long>, Event> eventMap = null;

    // Upper bound, so we don't fill the disk with monkey events
    private static final double MAX_EVENTS = 10000;
    private double maxEvents = MAX_EVENTS;

    private String dbFilename = "simianarmy_events";

    private String dbpassword = null;

    /** Constructor.
     *
     */
    public LocalDbRecorder(MonkeyConfiguration configuration) {
        if (configuration != null) {
            dbFilename = configuration.getStrOrElse("simianarmy.recorder.localdb.file", null);
            maxEvents = configuration.getNumOrElse("simianarmy.recorder.localdb.max_events", MAX_EVENTS);
            dbpassword = configuration.getStrOrElse("simianarmy.recorder.localdb.password", null);
        }
    }

    private synchronized void init() {
        if (nextId != null) {
            return;
        }
        File dbFile = null;
        dbFile = (dbFilename == null) ? tempDbFile() : new File(dbFilename);
        if (dbpassword != null) {
            db = DBMaker.newFileDB(dbFile)
                    .closeOnJvmShutdown()
                    .encryptionEnable(dbpassword)
                    .make();
        } else {
            db = DBMaker.newFileDB(dbFile)
                    .closeOnJvmShutdown()
                    .make();
        }
        eventMap = db.getTreeMap("eventMap");
        nextId = db.createAtomicLong("next", 1);
    }

    private static File tempDbFile() {
        try {
            final File tmpFile = File.createTempFile("mapdb", "db");
            tmpFile.deleteOnExit();
            return tmpFile;
        } catch (IOException e) {
            throw new RuntimeException("Temporary DB file could not be created", e);
        }
    }

    /* (non-Javadoc)
     * @see com.netflix.simianarmy.MonkeyRecorder#newEvent(MonkeyType, EventType, String, String)
     */
    @Override
    public Event newEvent(MonkeyType monkeyType, EventType eventType, String region,
            String id) {
        init();
        return new MapDbRecorderEvent(monkeyType, eventType, region, id);
    }

    /* (non-Javadoc)
     * @see com.netflix.simianarmy.MonkeyRecorder#recordEvent(com.netflix.simianarmy.MonkeyRecorder.Event)
     */
    @Override
    public void recordEvent(Event evt) {
        init();
        Fun.Tuple2<Long, Long> id = Fun.t2(evt.eventTime().getTime(),
                nextId.incrementAndGet());

        if (eventMap.size() + 1 > maxEvents) {
            eventMap.remove(eventMap.firstKey());
        }
        eventMap.put(id, evt);
        db.commit();
    }

    /* (non-Javadoc)
     * @see com.netflix.simianarmy.MonkeyRecorder#findEvents(java.util.Map, java.util.Date)
     */
    @Override
    public List<Event> findEvents(Map<String, String> query, Date after) {
        init();
        List<Event> foundEvents = new ArrayList<Event>();
        for (Event evt : eventMap.tailMap(toKey(after)).values()) {
            boolean matched = true;
            for (Map.Entry<String, String> pair : query.entrySet()) {
                if (pair.getKey().equals("id") && !evt.id().equals(pair.getValue())) {
                    matched = false;
                }
                if (pair.getKey().equals("monkeyType") && !evt.monkeyType().toString().equals(pair.getValue())) {
                    matched = false;
                }
                if (pair.getKey().equals("eventType") && !evt.eventType().toString().equals(pair.getValue())) {
                    matched = false;
                }
            }
            if (matched) {
                foundEvents.add(evt);
            }
        }
        return foundEvents;
    }

    /* (non-Javadoc)
     * @see com.netflix.simianarmy.MonkeyRecorder#findEvents(MonkeyType, Map, Date)
     */
    @Override
    public List<Event> findEvents(MonkeyType monkeyType, Map<String, String> query,
            Date after) {
        Map<String, String> copy = new LinkedHashMap<String, String>(query);
        copy.put("monkeyType", monkeyType.name());
        return findEvents(copy, after);
    }

    /* (non-Javadoc)
     * @see com.netflix.simianarmy.MonkeyRecorder#findEvents(MonkeyType, EventType, Map, Date)
     */
    @Override
    public List<Event> findEvents(MonkeyType monkeyType, EventType eventType,
            Map<String, String> query, Date after) {
        Map<String, String> copy = new LinkedHashMap<String, String>(query);
        copy.put("monkeyType", monkeyType.name());
        copy.put("eventType", eventType.name());
        return findEvents(copy, after);
    }

    private Fun.Tuple2<Long, Long> toKey(Date date) {
        return Fun.t2(date.getTime(), 0L);
    }

    /** Loggable event for LocalDbRecorder.
     *
     */
    public static class MapDbRecorderEvent implements MonkeyRecorder.Event, Serializable {
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

        private static final long serialVersionUID = 1L;

        /** Constructor.
         * @param monkeyType
         * @param eventType
         * @param region
         * @param id
         */
        public MapDbRecorderEvent(MonkeyType monkeyType, EventType eventType,
                String region, String id) {
            this.monkeyType = monkeyType;
            this.eventType = eventType;
            this.id = id;
            this.region = region;
            this.date = new Date();
        }

        /** Constructor.
         * @param monkeyType
         * @param eventType
         * @param region
         * @param id
         * @param time
         */
        public MapDbRecorderEvent(MonkeyType monkeyType, EventType eventType,
                String region, String id, long time) {
            this.monkeyType = monkeyType;
            this.eventType = eventType;
            this.id = id;
            this.region = region;
            this.date = new Date(time);
        }

        /* (non-Javadoc)
         * @see com.netflix.simianarmy.MonkeyRecorder.Event#id()
         */
        @Override
        public String id() {
            return id;
        }

        /* (non-Javadoc)
         * @see com.netflix.simianarmy.MonkeyRecorder.Event#eventTime()
         */
        @Override
        public Date eventTime() {
            return new Date(date.getTime());
        }

        /* (non-Javadoc)
         * @see com.netflix.simianarmy.MonkeyRecorder.Event#monkeyType()
         */
        @Override
        public MonkeyType monkeyType() {
            return monkeyType;
        }

        /* (non-Javadoc)
         * @see com.netflix.simianarmy.MonkeyRecorder.Event#eventType()
         */
        @Override
        public EventType eventType() {
            return eventType;
        }

        /* (non-Javadoc)
         * @see com.netflix.simianarmy.MonkeyRecorder.Event#region()
         */
        @Override
        public String region() {
            return region;
        }

        /* (non-Javadoc)
         * @see com.netflix.simianarmy.MonkeyRecorder.Event#fields()
         */
        @Override
        public Map<String, String> fields() {
            return Collections.unmodifiableMap(fields);
        }

        /* (non-Javadoc)
         * @see com.netflix.simianarmy.MonkeyRecorder.Event#field(java.lang.String)
         */
        @Override
        public String field(String name) {
            return fields.get(name);
        }

        /* (non-Javadoc)
         * @see com.netflix.simianarmy.MonkeyRecorder.Event#addField(java.lang.String, java.lang.String)
         */
        @Override
        public Event addField(String name, String value) {
            fields.put(name, value);
            return this;
        }

    }

    /** Appears to be used for testing, if so should be moved to a unit test. (2/16/2014, mgeis)
     * @param args
     */
    public static void main(String[] args) {
        LocalDbRecorder r = new LocalDbRecorder(null);
        r.init();
        List<Event> events2 = r.findEvents(new HashMap<String, String>(), new Date(0));
        for (Event event : events2) {
            System.out.println("Got:" + event + ": " + event.eventTime().getTime());
        }
        for (int i = 0; i < 10; i++) {
            Event event = r.newEvent(ChaosMonkey.Type.CHAOS,
                    ChaosMonkey.EventTypes.CHAOS_TERMINATION, "1", "1");
            r.recordEvent(event);
            System.out.println("Added:" + event + ": " + event.eventTime().getTime());
        }
        List<Event> events = r.findEvents(new HashMap<String, String>(), new Date(0));
        for (Event event : events) {
            System.out.println("Got:" + event + ": " + event.eventTime().getTime());
        }
    }
}

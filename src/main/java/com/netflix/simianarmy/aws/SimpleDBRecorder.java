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

import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collections;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.Attribute;

import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.basic.BasicRecorderEvent;

@SuppressWarnings("serial")
public class SimpleDBRecorder implements MonkeyRecorder {
    private AWSCredentials cred;
    private String region;
    private String domain;

    private enum Keys {
        id, eventTime, region, recordType, monkeyType, eventType;

        public static final Set<String> KEYSET = Collections.unmodifiableSet(new HashSet<String>() {
            {
                for (Keys k : Keys.values()) {
                    add(k.toString());
                }
            }
        });
    };

    public SimpleDBRecorder(String accessKey, String secretKey, String region, String domain) {
        this.cred = new BasicAWSCredentials(accessKey, secretKey);
        this.region = region;
        this.domain = domain;
    }

    public SimpleDBRecorder(AWSCredentials cred, String region, String domain) {
        this.cred = cred;
        this.region = region;
        this.domain = domain;
    }

    /**
     * Use {@link DefaultAWSCredentialsProviderChain} to provide credentials.
     * @param region
     * @param domain
     */
    public SimpleDBRecorder(String region, String domain) {
        this(new DefaultAWSCredentialsProviderChain().getCredentials(), region, domain);
    }

    protected AmazonSimpleDB sdbClient() {
        AmazonSimpleDB client = new AmazonSimpleDBClient(cred);
        client.setEndpoint("sdb." + region + ".amazonaws.com");
        return client;
    }

    private static String enumToValue(Enum e) {
        return String.format("%s|%s", e.name(), e.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    private static Enum valueToEnum(String value) {
        // parts = [enum value, enum class type]
        String[] parts = value.split("\\|", 2);
        if (parts.length < 2) {
            throw new RuntimeException("value " + value + " does not appear to be an internal enum format");
        }

        Class enumClass;
        try {
            enumClass = Class.forName(parts[1]);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("class for enum value " + value + " not found");
        }
        if (enumClass.isEnum()) {
            final Class<? extends Enum> enumSubClass = enumClass.asSubclass(Enum.class);
            return Enum.valueOf(enumSubClass, parts[0]);
        }
        throw new RuntimeException("value " + value + " does not appear to be an enum type");
    }

    public Event newEvent(Enum monkeyType, Enum eventType, String id) {
        return new BasicRecorderEvent(monkeyType, eventType, id);
    }

    public void recordEvent(Event evt) {
        List<ReplaceableAttribute> attrs = new LinkedList<ReplaceableAttribute>();
        attrs.add(new ReplaceableAttribute(Keys.id.name(), evt.id(), true));
        attrs.add(new ReplaceableAttribute(Keys.eventTime.name(), String.valueOf(evt.eventTime().getTime()), true));
        //TODO I think region is overloaded here, probably need to distinguish between the region being changed
        //and where the data is being stored.
        attrs.add(new ReplaceableAttribute(Keys.region.name(), region, true));
        attrs.add(new ReplaceableAttribute(Keys.recordType.name(), "MonkeyEvent", true));
        attrs.add(new ReplaceableAttribute(Keys.monkeyType.name(), enumToValue(evt.monkeyType()), true));
        attrs.add(new ReplaceableAttribute(Keys.eventType.name(), enumToValue(evt.eventType()), true));
        for (Map.Entry<String, String> pair : evt.fields().entrySet()) {
            if (pair.getValue() == null || pair.getValue().equals("") || Keys.KEYSET.contains(pair.getKey())) {
                continue;
            }
            attrs.add(new ReplaceableAttribute(pair.getKey(), pair.getValue(), true));
        }
        String pk = String.format("%s-%s-%s", evt.monkeyType().name(), evt.id(), region);
        PutAttributesRequest putReq = new PutAttributesRequest(domain, pk, attrs);
        sdbClient().putAttributes(putReq);

    }

    protected List<Event> findEvents(Map<String, String> queryMap, long after) {
        StringBuilder query = new StringBuilder(String.format("select * from %s where region = '%s'", domain, region));
        for (Map.Entry<String, String> pair : queryMap.entrySet()) {
            query.append(String.format(" and %s = '%s'", pair.getKey(), pair.getValue()));
        }
        if (after > 0) {
            query.append(String.format(" and eventTime > '%d'", after));
        }

        List<Event> list = new LinkedList<Event>();
        SelectRequest request = new SelectRequest(query.toString());
        request.setConsistentRead(Boolean.TRUE);

        SelectResult result = new SelectResult();
        do {
            result = sdbClient().select(request.withNextToken(result.getNextToken()));
            for (Item item : result.getItems()) {
                Map<String, String> fields = new HashMap<String, String>();
                Map<String, String> res = new HashMap<String, String>();
                for (Attribute attr : item.getAttributes()) {
                    if (Keys.KEYSET.contains(attr.getName())) {
                        res.put(attr.getName(), attr.getValue());
                    } else {
                        fields.put(attr.getName(), attr.getValue());
                    }
                }
                String eid = res.get(Keys.id.name());
                Enum monkeyType = valueToEnum(res.get(Keys.monkeyType.name()));
                Enum eventType = valueToEnum(res.get(Keys.eventType.name()));
                long eventTime = Long.parseLong(res.get(Keys.eventTime.name()));
                list.add(new BasicRecorderEvent(monkeyType, eventType, eid, eventTime).addFields(fields));
            }
        } while (result.getNextToken() != null);
        return list;
    }

    public List<Event> findEvents(Map<String, String> query, Date after) {
        return findEvents(query, after.getTime());
    }

    public List<Event> findEvents(Enum monkeyType, Map<String, String> query, Date after) {
        Map<String, String> copy = new LinkedHashMap<String, String>(query);
        copy.put(Keys.monkeyType.name(), enumToValue(monkeyType));
        return findEvents(copy, after);
    }

    public List<Event> findEvents(Enum monkeyType, Enum eventType, Map<String, String> query, Date after) {
        Map<String, String> copy = new LinkedHashMap<String, String>(query);
        copy.put(Keys.monkeyType.name(), enumToValue(monkeyType));
        copy.put(Keys.eventType.name(), enumToValue(eventType));
        return findEvents(copy, after);
    }
}

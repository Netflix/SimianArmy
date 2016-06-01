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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyType;
import com.netflix.simianarmy.NamedType;
import com.netflix.simianarmy.basic.BasicRecorderEvent;
import com.netflix.simianarmy.client.aws.AWSClient;

/**
 * The Class SimpleDBRecorder. Records events to and fetched events from a Amazon SimpleDB table (default SIMIAN_ARMY)
 */
@SuppressWarnings("serial")
public class SimpleDBRecorder implements MonkeyRecorder {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDBRecorder.class);

    private final AmazonSimpleDB simpleDBClient;

    private final String region;

    /** The domain. */
    private final String domain;

    /**
     * The Enum Keys.
     */
    private enum Keys {

        /** The event id. */
        id,
        /** The event time. */
        eventTime,
        /** The region. */
        region,
        /** The record type. */
        recordType,
        /** The monkey type. */
        monkeyType,
        /** The event type. */
        eventType;

        /** The Constant KEYSET. */
        public static final Set<String> KEYSET = Collections.unmodifiableSet(new HashSet<String>() {
            {
                for (Keys k : Keys.values()) {
                    add(k.toString());
                }
            }
        });
    };

    /**
     * Instantiates a new simple db recorder.
     *
     * @param awsClient
     *            the AWS client
     * @param domain
     *            the domain
     */
    public SimpleDBRecorder(AWSClient awsClient, String domain) {
        Validate.notNull(awsClient);
        Validate.notNull(domain);
        this.simpleDBClient = awsClient.sdbClient();
        this.region = awsClient.region();
        this.domain = domain;
    }

    /**
     * simple client. abstracted to aid testing
     *
     * @return the amazon simple db
     */
    protected AmazonSimpleDB sdbClient() {
        return simpleDBClient;
    }

    /**
     * Enum to value. Converts an enum to "name|type" string
     *
     * @param e
     *            the e
     * @return the string
     */
    public static String enumToValue(NamedType e) {
        return String.format("%s|%s", e.name(), e.getClass().getName());
    }

    /**
     * Value to enum. Converts a "name|type" string back to an enum.
     *
     * @param value
     *            the value
     * @return the enum
     */
    public static <T extends NamedType> T valueToEnum(
            Class<T> type, String value) {
        // parts = [enum value, enum class type]
        String[] parts = value.split("\\|", 2);
        if (parts.length < 2) {
            throw new RuntimeException("value " + value + " does not appear to be an internal enum format");
        }

        Class<?> enumClass;
        try {
            enumClass = Class.forName(parts[1]);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("class for enum value " + value + " not found");
        }
        if (!enumClass.isEnum()) {
            throw new RuntimeException("value " + value + " does not appear to be of an enum type");
        }
        if (!type.isAssignableFrom(enumClass)) {
            throw new RuntimeException("value " + value + " cannot be assigned to a variable of this type: "
                    + type.getCanonicalName());
        }
        @SuppressWarnings("rawtypes")
        Class<? extends Enum> enumType = enumClass.asSubclass(Enum.class);
        @SuppressWarnings("unchecked")
        T enumValue = (T) Enum.valueOf(enumType, parts[0]);
        return enumValue;
    }

    /** {@inheritDoc} */
    @Override
    public Event newEvent(MonkeyType monkeyType, EventType eventType, String reg, String id) {
        return new BasicRecorderEvent(monkeyType, eventType, reg, id);
    }

    /** {@inheritDoc} */
    @Override
    public void recordEvent(Event evt) {
        String evtTime = String.valueOf(evt.eventTime().getTime());
        List<ReplaceableAttribute> attrs = new LinkedList<ReplaceableAttribute>();
        attrs.add(new ReplaceableAttribute(Keys.id.name(), evt.id(), true));
        attrs.add(new ReplaceableAttribute(Keys.eventTime.name(), evtTime, true));
        attrs.add(new ReplaceableAttribute(Keys.region.name(), evt.region(), true));
        attrs.add(new ReplaceableAttribute(Keys.recordType.name(), "MonkeyEvent", true));
        attrs.add(new ReplaceableAttribute(Keys.monkeyType.name(), enumToValue(evt.monkeyType()), true));
        attrs.add(new ReplaceableAttribute(Keys.eventType.name(), enumToValue(evt.eventType()), true));
        for (Map.Entry<String, String> pair : evt.fields().entrySet()) {
            if (pair.getValue() == null || pair.getValue().equals("") || Keys.KEYSET.contains(pair.getKey())) {
                continue;
            }
            attrs.add(new ReplaceableAttribute(pair.getKey(), pair.getValue(), true));
        }
        // Let pk contain the timestamp so that the same resource can have multiple events.
        String pk = String.format("%s-%s-%s-%s", evt.monkeyType().name(), evt.id(), region, evtTime);
        PutAttributesRequest putReq = new PutAttributesRequest(domain, pk, attrs);
        sdbClient().putAttributes(putReq);

    }

    /**
     * Find events.
     *
     * @param queryMap
     *            the query map
     * @param after
     *            the start time to query for all events after
     * @return the list
     */
    protected List<Event> findEvents(Map<String, String> queryMap, long after) {
        StringBuilder query = new StringBuilder(
                String.format("select * from `%s` where region = '%s'", domain, region));
        for (Map.Entry<String, String> pair : queryMap.entrySet()) {
            query.append(String.format(" and %s = '%s'", pair.getKey(), pair.getValue()));
        }
        query.append(String.format(" and eventTime > '%d'", after));
        // always return with most recent record first
        query.append(" order by eventTime desc");

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
                String ereg = res.get(Keys.region.name());
                MonkeyType monkeyType = valueToEnum(MonkeyType.class, res.get(Keys.monkeyType.name()));
                EventType eventType = valueToEnum(EventType.class, res.get(Keys.eventType.name()));
                long eventTime = Long.parseLong(res.get(Keys.eventTime.name()));
                list.add(new BasicRecorderEvent(monkeyType, eventType, ereg, eid, eventTime).addFields(fields));
            }
        } while (result.getNextToken() != null);
        return list;
    }

    /** {@inheritDoc} */
    @Override
    public List<Event> findEvents(Map<String, String> query, Date after) {
        return findEvents(query, after.getTime());
    }

    /** {@inheritDoc} */
    @Override
    public List<Event> findEvents(MonkeyType monkeyType, Map<String, String> query, Date after) {
        Map<String, String> copy = new LinkedHashMap<String, String>(query);
        copy.put(Keys.monkeyType.name(), enumToValue(monkeyType));
        return findEvents(copy, after);
    }

    /** {@inheritDoc} */
    @Override
    public List<Event> findEvents(MonkeyType monkeyType, EventType eventType, Map<String, String> query, Date after) {
        Map<String, String> copy = new LinkedHashMap<String, String>(query);
        copy.put(Keys.monkeyType.name(), enumToValue(monkeyType));
        copy.put(Keys.eventType.name(), enumToValue(eventType));
        return findEvents(copy, after);
    }

    /**
     * Creates the SimpleDB domain, if it does not already exist.
     */
    public void init() {
        try {
            if (this.region == null || this.region.equals("region-null")) {
                // This is a mock with an invalid region; avoid a slow timeout
                LOGGER.debug("Region=null; skipping SimpleDB domain creation");
                return;
            }
            ListDomainsResult listDomains = sdbClient().listDomains();
            for (String d : listDomains.getDomainNames()) {
                if (d.equals(domain)) {
                    LOGGER.debug("SimpleDB domain found: {}", domain);
                    return;
                }
            }
            LOGGER.info("Creating SimpleDB domain: {}", domain);
            CreateDomainRequest createDomainRequest = new CreateDomainRequest(
                    domain);
            sdbClient().createDomain(createDomainRequest);
        } catch (AmazonClientException e) {
            LOGGER.warn("Error while trying to auto-create SimpleDB domain", e);
        }
    }
}

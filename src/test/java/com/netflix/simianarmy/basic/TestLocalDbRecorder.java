package com.netflix.simianarmy.basic;

import com.google.common.collect.ImmutableMap;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class TestLocalDbRecorder {

    private LocalDbRecorder localDbRecorder = new LocalDbRecorder(new BasicConfiguration(new Properties()));

    @Test
    public void testId() {
        MonkeyRecorder.Event evt = makeEvent();
        localDbRecorder.recordEvent(evt);

        Map<String, String> query = ImmutableMap.of("id", evt.id());
        List<MonkeyRecorder.Event> evts = performQuery(query);

        assertEquals(evts.size(), 1);
    }

    @Test
    public void testIdNoMatch() {
        MonkeyRecorder.Event evt = makeEvent();
        localDbRecorder.recordEvent(evt);

        Map<String, String> query = ImmutableMap.of("id", "wrong-id");
        List<MonkeyRecorder.Event> evts = performQuery(query);

        assertEquals(evts.size(), 0);
    }

    @Test
    public void testEventTime() {
        MonkeyRecorder.Event evt = makeEvent();
        localDbRecorder.recordEvent(evt);

        Map<String, String> query = ImmutableMap.of("eventTime", Long.toString(evt.eventTime().getTime()));
        List<MonkeyRecorder.Event> evts = performQuery(query);

        assertEquals(evts.size(), 1);
    }

    @Test
    public void testEventTimeNoMatch() {
        MonkeyRecorder.Event evt = makeEvent();
        localDbRecorder.recordEvent(evt);

        Map<String, String> query = ImmutableMap.of("eventTime", Long.toString(evt.eventTime().getTime()-1));
        List<MonkeyRecorder.Event> evts = performQuery(query);

        assertEquals(evts.size(), 0);
    }

    @Test
    public void testRegionMatch() {
        MonkeyRecorder.Event evt = makeEvent("region-1");
        localDbRecorder.recordEvent(evt);

        Map<String, String> query = ImmutableMap.of("region", "region-1");
        List<MonkeyRecorder.Event> evts = performQuery(query);

        assertEquals(evts.size(), 1);
    }

    @Test
    public void testRegionNoMatch() {
        MonkeyRecorder.Event evt = makeEvent();
        localDbRecorder.recordEvent(evt);

        Map<String, String> query = ImmutableMap.of("region", "some-other-region");
        List<MonkeyRecorder.Event> evts = performQuery(query);

        assertEquals(evts.size(), 0);
    }

    @Test
    public void testCustomFieldMatch() {
        MonkeyRecorder.Event evt = makeEvent();
        evt.addField("groupName", "group-name-1");
        localDbRecorder.recordEvent(evt);

        Map<String, String> query = ImmutableMap.of("groupName", "group-name-1");
        List<MonkeyRecorder.Event> evts = performQuery(query);

        assertEquals(evts.size(), 1);
    }

    @Test
    public void testCustomFieldNoMatch() {
        MonkeyRecorder.Event evt = makeEvent();
        evt.addField("groupName", "group-name-1");
        localDbRecorder.recordEvent(evt);

        Map<String, String> query = ImmutableMap.of("groupName", "some-other-group-name");
        List<MonkeyRecorder.Event> evts = performQuery(query);

        assertEquals(evts.size(), 0);
    }

    @Test
    public void testCustomFieldNotPresent() {
        MonkeyRecorder.Event evt = makeEvent();
        localDbRecorder.recordEvent(evt);

        Map<String, String> query = ImmutableMap.of("groupName", "group-name-2");
        List<MonkeyRecorder.Event> evts = performQuery(query);

        assertEquals(evts.size(), 0);
    }

    @Test
    public void testMultipleEventsMatch() {
        final int numberOfEvents = 10;
        for (int i = 0; i < numberOfEvents; i++) {
            MonkeyRecorder.Event evt = makeEvent();
            evt.addField("groupName", "group-name-3");
            localDbRecorder.recordEvent(evt);
        }

        Map<String, String> query = ImmutableMap.of("groupName", "group-name-3");
        List<MonkeyRecorder.Event> evts = performQuery(query);

        assertEquals(evts.size(), numberOfEvents);
    }

    private List<MonkeyRecorder.Event> performQuery(Map<String, String> query) {
        return localDbRecorder.findEvents(ChaosMonkey.Type.CHAOS, ChaosMonkey.EventTypes.CHAOS_TERMINATION, query, new Date(0));
    }

    private MonkeyRecorder.Event makeEvent(String region) {
        return localDbRecorder.newEvent(ChaosMonkey.Type.CHAOS, ChaosMonkey.EventTypes.CHAOS_TERMINATION, region,
                                        UUID.randomUUID().toString());
    }

    private MonkeyRecorder.Event makeEvent() {
        return makeEvent("some-region");
    }
}
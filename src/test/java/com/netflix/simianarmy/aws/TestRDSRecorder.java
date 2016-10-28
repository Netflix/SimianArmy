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
// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.aws;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.MonkeyType;
import com.netflix.simianarmy.basic.BasicRecorderEvent;

// CHECKSTYLE IGNORE MagicNumberCheck
public class TestRDSRecorder extends RDSRecorder {

    private static final String REGION = "us-west-1";
	
    public TestRDSRecorder() {
    	super(mock(JdbcTemplate.class), "recordertable", REGION);
    }

    public enum Type implements MonkeyType {
        MONKEY
    }

    public enum EventTypes implements EventType {
        EVENT
    }
    
    @Test
    public void testInit() {        
        TestRDSRecorder recorder = new TestRDSRecorder();	
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(recorder.getJdbcTemplate()).execute(sqlCap.capture());
        recorder.init();        
        Assert.assertEquals(sqlCap.getValue(), "create table if not exists recordertable ( eventId varchar(255), eventTime BIGINT, monkeyType varchar(255), eventType varchar(255), region varchar(255), dataJson varchar(4096) )");
    }    

	@SuppressWarnings("unchecked")
    @Test
    public void testInsertNewRecordEvent() {
    	// mock the select query that is issued to see if the record already exists
        ArrayList<Event> events = new ArrayList<>();
        TestRDSRecorder recorder = new TestRDSRecorder();
		when(recorder.getJdbcTemplate().query(Matchers.anyString(), 
         		                            Matchers.any(Object[].class), 
         		                            Matchers.any(RowMapper.class))).thenReturn(events);

		Event evt = newEvent(Type.MONKEY, EventTypes.EVENT, "region", "testId");
        evt.addField("field1", "value1");
        evt.addField("field2", "value2");
        
        // this will be ignored as it conflicts with reserved key
        evt.addField("id", "ignoreThis");
        
        ArgumentCaptor<Object> objCap = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        when(recorder.getJdbcTemplate().update(sqlCap.capture(), 
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture())).thenReturn(1);
        recorder.recordEvent(evt);
        List<Object> args = objCap.getAllValues();        
        Assert.assertEquals(sqlCap.getValue(), "insert into recordertable (eventId,eventTime,monkeyType,eventType,region,dataJson) values (?,?,?,?,?,?)");
        Assert.assertEquals(args.size(), 6);
        Assert.assertEquals(args.get(0).toString(), evt.id());
        Assert.assertEquals(args.get(1).toString(), evt.eventTime().getTime() + "");
        Assert.assertEquals(args.get(2).toString(), SimpleDBRecorder.enumToValue(evt.monkeyType()));
        Assert.assertEquals(args.get(3).toString(), SimpleDBRecorder.enumToValue(evt.eventType()));
        Assert.assertEquals(args.get(4).toString(), evt.region());
    }

    private Event mkSelectResult(String id, Event evt) {
    	evt.addField("field1", "value1");
    	evt.addField("field2", "value2");
        return evt;
    }

	@SuppressWarnings("unchecked")
    @Test
    public void testFindEvent() {    	
        Event evt1 = new BasicRecorderEvent(Type.MONKEY, EventTypes.EVENT, "region", "testId1", 1330538400000L);
        mkSelectResult("testId1", evt1);
        Event evt2 = new BasicRecorderEvent(Type.MONKEY, EventTypes.EVENT, "region", "testId2", 1330538400000L);
        mkSelectResult("testId2", evt2);
        
        ArrayList<Event> events = new ArrayList<>();
        TestRDSRecorder recorder = new TestRDSRecorder();
        events.add(evt1);
        events.add(evt2);
		when(recorder.getJdbcTemplate().query(Matchers.anyString(), 
				Matchers.argThat(new ArgumentMatcher<Object []>(){
					@Override
					public boolean matches(Object argument) {
						Object [] args = (Object [])argument;
						Assert.assertTrue(args[0] instanceof String);
						Assert.assertEquals((String)args[0],REGION);
						return true;
					}
					
				}), 
         		Matchers.any(RowMapper.class))).thenReturn(events);
        
        Map<String, String> query = new LinkedHashMap<String, String>();
        query.put("instanceId", "testId1");

        verifyEvents(recorder.findEvents(query, new Date(0)));
    }

    void verifyEvents(List<Event> events) {
        Assert.assertEquals(events.size(), 2);

        Assert.assertEquals(events.get(0).id(), "testId1");
        Assert.assertEquals(events.get(0).eventTime().getTime(), 1330538400000L);
        Assert.assertEquals(events.get(0).monkeyType(), Type.MONKEY);
        Assert.assertEquals(events.get(0).eventType(), EventTypes.EVENT);
        Assert.assertEquals(events.get(0).field("field1"), "value1");
        Assert.assertEquals(events.get(0).field("field2"), "value2");
        Assert.assertEquals(events.get(0).fields().size(), 2);

        Assert.assertEquals(events.get(1).id(), "testId2");
        Assert.assertEquals(events.get(1).eventTime().getTime(), 1330538400000L);
        Assert.assertEquals(events.get(1).monkeyType(), Type.MONKEY);
        Assert.assertEquals(events.get(1).eventType(), EventTypes.EVENT);
        Assert.assertEquals(events.get(1).field("field1"), "value1");
        Assert.assertEquals(events.get(1).field("field2"), "value2");
        Assert.assertEquals(events.get(1).fields().size(), 2);
    }

	@SuppressWarnings("unchecked")
    @Test
    public void testFindEventNotFound() {
        ArrayList<Event> events = new ArrayList<>();
        TestRDSRecorder recorder = new TestRDSRecorder();
		when(recorder.getJdbcTemplate().query(Matchers.anyString(), 
				Matchers.argThat(new ArgumentMatcher<Object []>(){
					@Override
					public boolean matches(Object argument) {
						Object [] args = (Object [])argument;
						Assert.assertTrue(args[0] instanceof String);
						Assert.assertEquals((String)args[0],REGION);
						return true;
					}
					
				}), 
         		Matchers.any(RowMapper.class))).thenReturn(events);
		
		List<Event> results = recorder.findEvents(new HashMap<String, String>(), new Date());
		Assert.assertEquals(results.size(), 0);
    }
    
}

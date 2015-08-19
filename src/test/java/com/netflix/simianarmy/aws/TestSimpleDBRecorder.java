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

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.MonkeyType;
import com.netflix.simianarmy.client.aws.AWSClient;

// CHECKSTYLE IGNORE MagicNumberCheck
public class TestSimpleDBRecorder extends SimpleDBRecorder {

    private static AWSClient makeMockAWSClient() {
        AmazonSimpleDB sdbMock = mock(AmazonSimpleDB.class);
        AWSClient awsClient = mock(AWSClient.class);
        when(awsClient.sdbClient()).thenReturn(sdbMock);
        when(awsClient.region()).thenReturn("region");
        return awsClient;
    }

    public TestSimpleDBRecorder() {
        super(makeMockAWSClient(), "DOMAIN");
        sdbMock = super.sdbClient();
    }

    private final AmazonSimpleDB sdbMock;

    @Override
    protected AmazonSimpleDB sdbClient() {
        return sdbMock;
    }

    protected AmazonSimpleDB superSdbClient() {
        return super.sdbClient();
    }

    @Test
    public void testClients() {
        TestSimpleDBRecorder recorder1 = new TestSimpleDBRecorder();
        Assert.assertNotNull(recorder1.superSdbClient(), "non null super sdbClient");

        TestSimpleDBRecorder recorder2 = new TestSimpleDBRecorder();
        Assert.assertNotNull(recorder2.superSdbClient(), "non null super sdbClient");
    }

    public enum Type implements MonkeyType {
        MONKEY
    }

    public enum EventTypes implements EventType {
        EVENT
    }

    @Test
    public void testRecordEvent() {
        ArgumentCaptor<PutAttributesRequest> arg = ArgumentCaptor.forClass(PutAttributesRequest.class);

        Event evt = newEvent(Type.MONKEY, EventTypes.EVENT, "region", "testId");
        evt.addField("field1", "value1");
        evt.addField("field2", "value2");
        // this will be ignored as it conflicts with reserved key
        evt.addField("id", "ignoreThis");

        recordEvent(evt);

        verify(sdbMock).putAttributes(arg.capture());

        PutAttributesRequest req = arg.getValue();
        Assert.assertEquals(req.getDomainName(), "DOMAIN");
        Assert.assertEquals(req.getItemName(), "MONKEY-testId-region-" + evt.eventTime().getTime());
        Map<String, String> map = new HashMap<String, String>();
        for (ReplaceableAttribute attr : req.getAttributes()) {
            map.put(attr.getName(), attr.getValue());
        }

        Assert.assertEquals(map.remove("id"), "testId");
        Assert.assertEquals(map.remove("eventTime"), String.valueOf(evt.eventTime().getTime()));
        Assert.assertEquals(map.remove("region"), "region");
        Assert.assertEquals(map.remove("recordType"), "MonkeyEvent");
        Assert.assertEquals(map.remove("monkeyType"), "MONKEY|com.netflix.simianarmy.aws.TestSimpleDBRecorder$Type");
        Assert.assertEquals(map.remove("eventType"),
            "EVENT|com.netflix.simianarmy.aws.TestSimpleDBRecorder$EventTypes");
        Assert.assertEquals(map.remove("field1"), "value1");
        Assert.assertEquals(map.remove("field2"), "value2");
        Assert.assertEquals(map.size(), 0);
    }

    private SelectResult mkSelectResult(String id) {
        Item item = new Item();
        List<Attribute> attrs = new LinkedList<Attribute>();
        attrs.add(new Attribute("id", id));
        attrs.add(new Attribute("eventTime", "1330538400000"));
        attrs.add(new Attribute("region", "region"));
        attrs.add(new Attribute("recordType", "MonkeyEvent"));
        attrs.add(new Attribute("monkeyType", "MONKEY|com.netflix.simianarmy.aws.TestSimpleDBRecorder$Type"));
        attrs.add(new Attribute("eventType", "EVENT|com.netflix.simianarmy.aws.TestSimpleDBRecorder$EventTypes"));
        attrs.add(new Attribute("field1", "value1"));
        attrs.add(new Attribute("field2", "value2"));
        item.setAttributes(attrs);
        item.setName("MONKEY-" + id + "-region");
        SelectResult result = new SelectResult();
        result.setItems(Arrays.asList(item));
        return result;
    }

    @Test
    public void testFindEvent() {
        SelectResult result1 = mkSelectResult("testId1");
        result1.setNextToken("nextToken");
        SelectResult result2 = mkSelectResult("testId2");

        ArgumentCaptor<SelectRequest> arg = ArgumentCaptor.forClass(SelectRequest.class);

        when(sdbMock.select(any(SelectRequest.class))).thenReturn(result1).thenReturn(result2);

        Map<String, String> query = new LinkedHashMap<String, String>();
        query.put("instanceId", "testId1");

        verifyEvents(findEvents(query, new Date(0)));

        verify(sdbMock, times(2)).select(arg.capture());
        SelectRequest req = arg.getValue();
        StringBuilder sb = new StringBuilder();
        sb.append("select * from `DOMAIN` where region = 'region'");
        sb.append(" and instanceId = 'testId1'");

        Assert.assertEquals(req.getSelectExpression(), sb.toString() + " and eventTime > '0' order by eventTime desc");

        // reset for next test
        when(sdbMock.select(any(SelectRequest.class))).thenReturn(result1).thenReturn(result2);

        verifyEvents(findEvents(Type.MONKEY, query, new Date(0)));

        verify(sdbMock, times(4)).select(arg.capture());
        req = arg.getValue();
        sb.append(" and monkeyType = 'MONKEY|com.netflix.simianarmy.aws.TestSimpleDBRecorder$Type'");
        Assert.assertEquals(req.getSelectExpression(), sb.toString() + " and eventTime > '0' order by eventTime desc");

        // reset for next test
        when(sdbMock.select(any(SelectRequest.class))).thenReturn(result1).thenReturn(result2);

        verifyEvents(findEvents(Type.MONKEY, EventTypes.EVENT, query, new Date(0)));

        verify(sdbMock, times(6)).select(arg.capture());
        req = arg.getValue();
        sb.append(" and eventType = 'EVENT|com.netflix.simianarmy.aws.TestSimpleDBRecorder$EventTypes'");
        Assert.assertEquals(req.getSelectExpression(), sb.toString() + " and eventTime > '0' order by eventTime desc");

        // reset for next test
        when(sdbMock.select(any(SelectRequest.class))).thenReturn(result1).thenReturn(result2);

        verifyEvents(findEvents(Type.MONKEY, EventTypes.EVENT, query, new Date(1330538400000L)));

        verify(sdbMock, times(8)).select(arg.capture());
        req = arg.getValue();
        sb.append(" and eventTime > '1330538400000' order by eventTime desc");
        Assert.assertEquals(req.getSelectExpression(), sb.toString());
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
}

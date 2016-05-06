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
//CHECKSTYLE IGNORE MagicNumber
package com.netflix.simianarmy.resources.chaos;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.MonkeyType;
import com.netflix.simianarmy.basic.BasicRecorderEvent;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class TestChaosMonkeyResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestChaosMonkeyResource.class);

    @Captor
    private ArgumentCaptor<MonkeyType> monkeyTypeArg;
    @Captor
    private ArgumentCaptor<EventType> eventTypeArg;
    @Captor
    private ArgumentCaptor<Map<String, String>> queryArg;
    @Captor
    private ArgumentCaptor<Date> dateArg;

    @Mock
    private UriInfo mockUriInfo;
    @Mock
    private static MonkeyRecorder mockRecorder;

    @BeforeTest
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testTerminateNow() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("ondemandTermination.properties");

        String input = "{\"eventType\":\"CHAOS_TERMINATION\",\"groupType\":\"TYPE_C\",\"groupName\":\"name4\"}";

        Assert.assertEquals(ctx.selectedOn().size(), 0);
        Assert.assertEquals(ctx.terminated().size(), 0);

        ChaosMonkeyResource resource = new ChaosMonkeyResource(new BasicChaosMonkey(ctx));
        validateAddEventResult(resource, input, Response.Status.OK);
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);

        validateAddEventResult(resource, input, Response.Status.OK);
        Assert.assertEquals(ctx.selectedOn().size(), 2);
        Assert.assertEquals(ctx.terminated().size(), 2);

        // TYPE_C.name4 only has two instances, so the 3rd ondemand termination
        // will not terminate anything.
        validateAddEventResult(resource, input, Response.Status.GONE);
        Assert.assertEquals(ctx.selectedOn().size(), 3);
        Assert.assertEquals(ctx.terminated().size(), 2);

        // Try a different type will work
        input = "{\"eventType\":\"CHAOS_TERMINATION\",\"groupType\":\"TYPE_C\",\"groupName\":\"name5\"}";
        validateAddEventResult(resource, input, Response.Status.OK);
        Assert.assertEquals(ctx.selectedOn().size(), 4);
        Assert.assertEquals(ctx.terminated().size(), 3);
    }

    @Test
    void testTerminateNowDisabled() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("ondemandTerminationDisabled.properties");
        String input = "{\"eventType\":\"CHAOS_TERMINATION\",\"groupType\":\"TYPE_C\",\"groupName\":\"name4\"}";

        Assert.assertEquals(ctx.selectedOn().size(), 0);
        Assert.assertEquals(ctx.terminated().size(), 0);

        ChaosMonkeyResource resource = new ChaosMonkeyResource(new BasicChaosMonkey(ctx));
        validateAddEventResult(resource, input, Response.Status.FORBIDDEN);
        Assert.assertEquals(ctx.selectedOn().size(), 0);
        Assert.assertEquals(ctx.terminated().size(), 0);
    }

    @Test
    void testTerminateNowBadInput() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("ondemandTermination.properties");
        String input = "{\"groupType\":\"TYPE_C\",\"groupName\":\"name4\"}";

        ChaosMonkeyResource resource = new ChaosMonkeyResource(new BasicChaosMonkey(ctx));
        validateAddEventResult(resource, input, Response.Status.BAD_REQUEST);

        input = "{\"eventType\":\"CHAOS_TERMINATION\",\"groupName\":\"name4\"}";
        resource = new ChaosMonkeyResource(new BasicChaosMonkey(ctx));
        validateAddEventResult(resource, input, Response.Status.BAD_REQUEST);

        input = "{\"eventType\":\"CHAOS_TERMINATION\",\"groupType\":\"TYPE_C\"}";
        resource = new ChaosMonkeyResource(new BasicChaosMonkey(ctx));
        validateAddEventResult(resource, input, Response.Status.BAD_REQUEST);
    }

    @Test
    void testTerminateNowBadGroupNotExist() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("ondemandTermination.properties");

        String input = "{\"eventType\":\"CHAOS_TERMINATION\",\"groupType\":\"INVALID\",\"groupName\":\"name4\"}";
        ChaosMonkeyResource resource = new ChaosMonkeyResource(new BasicChaosMonkey(ctx));
        validateAddEventResult(resource, input, Response.Status.NOT_FOUND);

        input = "{\"eventType\":\"CHAOS_TERMINATION\",\"groupType\":\"TYPE_C\",\"groupName\":\"INVALID\"}";
        resource = new ChaosMonkeyResource(new BasicChaosMonkey(ctx));
        validateAddEventResult(resource, input, Response.Status.NOT_FOUND);
    }

    @Test
    void testTerminateNowBadEventType() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("ondemandTermination.properties");

        String input = "{\"eventType\":\"INVALID\",\"groupType\":\"TYPE_C\",\"groupName\":\"name4\"}";
        ChaosMonkeyResource resource = new ChaosMonkeyResource(new BasicChaosMonkey(ctx));
        validateAddEventResult(resource, input, Response.Status.BAD_REQUEST);
    }

    @Test
    public void testResource() {
        MonkeyRunner.getInstance().replaceMonkey(BasicChaosMonkey.class, MockTestChaosMonkeyContext.class);

        ChaosMonkeyResource resource = new ChaosMonkeyResource();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("groupType", "ASG");
        Date queryDate = new Date();
        queryParams.add("since", String.valueOf(queryDate.getTime()));

        when(mockUriInfo.getQueryParameters()).thenReturn(queryParams);

        @SuppressWarnings("unchecked")
        // fix when Matcher.anyMapOf is available
        Map<String, String> anyMap = anyMap();

        when(mockRecorder.findEvents(any(MonkeyType.class), any(EventType.class), anyMap, any(Date.class))).thenReturn(
                Arrays.asList(mkEvent("i-123456789012345670"), mkEvent("i-123456789012345671")));

        try {
            Response resp = resource.getChaosEvents(mockUriInfo);
            Assert.assertEquals(resp.getEntity().toString(), getResource("getChaosEventsResponse.json"));
        } catch (Exception e) {
            LOGGER.error("exception from getChaosEvents", e);
            Assert.fail("getChaosEvents throws exception");
        }

        verify(mockRecorder).findEvents(monkeyTypeArg.capture(), eventTypeArg.capture(), queryArg.capture(),
                dateArg.capture());

        Assert.assertEquals(monkeyTypeArg.getValue(), ChaosMonkey.Type.CHAOS);
        Assert.assertEquals(eventTypeArg.getValue(), ChaosMonkey.EventTypes.CHAOS_TERMINATION);
        Map<String, String> query = queryArg.getValue();
        Assert.assertEquals(query.size(), 1);
        Assert.assertEquals(query.get("groupType"), "ASG");
        Assert.assertEquals(dateArg.getValue(), queryDate);
    }

    private MonkeyRecorder.Event mkEvent(String instance) {
        final MonkeyType monkeyType = ChaosMonkey.Type.CHAOS;
        final EventType eventType = ChaosMonkey.EventTypes.CHAOS_TERMINATION;
        // SUPPRESS CHECKSTYLE MagicNumber
        return new BasicRecorderEvent(monkeyType, eventType, "region", instance, 1330538400000L)
        .addField("groupType", "ASG").addField("groupName", "testGroup");
    }

    public static class MockTestChaosMonkeyContext extends TestChaosMonkeyContext {
        @Override
        public MonkeyRecorder recorder() {
            return mockRecorder;
        }
    }

    String getResource(String name) {
        // get resource as stream, use Scanner to read stream as one token
        return new Scanner(TestChaosMonkeyResource.class.getResourceAsStream(name), "UTF-8").useDelimiter("\\A").next();
    }

    private void validateAddEventResult(ChaosMonkeyResource resource, String input, Response.Status responseStatus) {
        try {
            Response resp = resource.addEvent(input);
            Assert.assertEquals(resp.getStatus(), responseStatus.getStatusCode());
        } catch (Exception e) {
            LOGGER.error("exception from addEvent", e);
            Assert.fail("addEvent throws exception");
        }
    }
}

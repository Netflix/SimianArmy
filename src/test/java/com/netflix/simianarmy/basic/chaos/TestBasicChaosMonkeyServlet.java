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
package com.netflix.simianarmy.basic.chaos;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Scanner;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.Mock;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.testng.Assert;

import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.basic.BasicRecorderEvent;

import com.netflix.simianarmy.chaos.TestChaosMonkeyContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestBasicChaosMonkeyServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestBasicChaosMonkeyServlet.class);

    @Captor
    private ArgumentCaptor<Enum> monkeyTypeArg;
    @Captor
    private ArgumentCaptor<Enum> eventTypeArg;
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
    public void testServlet() {
        MonkeyRunner.getInstance().replaceMonkey(BasicChaosMonkey.class, MockTestChaosMonkeyContext.class);

        BasicChaosMonkey.Servlet servlet = new BasicChaosMonkey.Servlet();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("groupType", "ASG");
        Date queryDate = new Date();
        queryParams.add("since", String.valueOf(queryDate.getTime()));

        when(mockUriInfo.getQueryParameters()).thenReturn(queryParams);

        @SuppressWarnings("unchecked")
        // fix when Matcher.anyMapOf is available
        Map<String, String> anyMap = anyMap();

        when(mockRecorder.findEvents(any(Enum.class), any(Enum.class), anyMap, any(Date.class))).thenReturn(
                Arrays.asList(mkEvent("i-1234356780"), mkEvent("i-123456781")));

        try {
            Response resp = servlet.getChaosEvents(mockUriInfo);
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
        final Enum monkeyType = ChaosMonkey.Type.CHAOS;
        final Enum eventType = ChaosMonkey.EventTypes.CHAOS_TERMINATION;
        // SUPPRESS CHECKSTYLE MagicNumber
        return new BasicRecorderEvent(monkeyType, eventType, "id", 1330538400000L).addField("instanceId", instance)
                .addField("groupType", "ASG").addField("groupName", "testGroup").addField("region", "us-east-1");
    }

    public static class MockTestChaosMonkeyContext extends TestChaosMonkeyContext {
        public MonkeyRecorder recorder() {
            return mockRecorder;
        }
    }

    String getResource(String name) {
        // get resource as stream, use Scanner to read stream as one token
        return new Scanner(TestBasicChaosMonkey.class.getResourceAsStream(name), "UTF-8").useDelimiter("\\A").next();
    }
}

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
package com.netflix.simianarmy.resources.chaos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.google.common.base.Strings;
import com.netflix.simianarmy.Monkey;
import com.sun.jersey.spi.resource.Singleton;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.FeatureNotEnabledException;
import com.netflix.simianarmy.InstanceGroupNotFoundException;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.NotFoundException;
import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.chaos.ChaosType;
import com.netflix.simianarmy.chaos.ShutdownInstanceChaosType;

/**
 * The Class ChaosMonkeyResource for json REST apis.
 */
@Path("/v1/chaos")
@Produces(MediaType.APPLICATION_JSON)
@Singleton
public class ChaosMonkeyResource {

    /** The Constant JSON_FACTORY. */
    private static final MappingJsonFactory JSON_FACTORY = new MappingJsonFactory();

    /** The monkey. */
    private ChaosMonkey monkey = null;

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ChaosMonkeyResource.class);

    /**
     * Instantiates a chaos monkey resource with a specific chaos monkey.
     *
     * @param monkey
     *          the chaos monkey
     */
    public ChaosMonkeyResource(ChaosMonkey monkey) {
        this.monkey = monkey;
    }

    /**
     * Instantiates a chaos monkey resource using a registered chaos monkey from factory.
     */
    public ChaosMonkeyResource() {
        for (Monkey runningMonkey : MonkeyRunner.getInstance().getMonkeys()) {
            if (runningMonkey instanceof ChaosMonkey) {
                this.monkey = (ChaosMonkey) runningMonkey;
                break;
            }
        }
        if (this.monkey == null) {
            LOGGER.info("Creating a new Chaos monkey instance for the resource.");
            this.monkey = MonkeyRunner.getInstance().factory(ChaosMonkey.class);
        }
    }

    /**
     * Gets the chaos events. Creates GET /api/v1/chaos api which outputs the chaos events in json. Users can specify
     * cgi query params to filter the results and use "since" query param to set the start of a timerange. "since" should
     * be specified in milliseconds since the epoch.
     *
     * @param uriInfo
     *            the uri info
     * @return the chaos events json response
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @GET
    public Response getChaosEvents(@Context UriInfo uriInfo) throws IOException {
        Map<String, String> query = new HashMap<String, String>();
        Date date = null;
        for (Map.Entry<String, List<String>> pair : uriInfo.getQueryParameters().entrySet()) {
            if (pair.getValue().isEmpty()) {
                continue;
            }
            if (pair.getKey().equals("since")) {
                date = new Date(Long.parseLong(pair.getValue().get(0)));
            } else {
                query.put(pair.getKey(), pair.getValue().get(0));
            }
        }
        // if "since" not set, default to 24 hours ago
        if (date == null) {
            Calendar now = monkey.context().calendar().now();
            now.add(Calendar.DAY_OF_YEAR, -1);
            date = now.getTime();
        }

        List<Event> evts = monkey.context().recorder()
                .findEvents(ChaosMonkey.Type.CHAOS, ChaosMonkey.EventTypes.CHAOS_TERMINATION, query, date);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);
        gen.writeStartArray();
        for (Event evt : evts) {
            gen.writeStartObject();
            gen.writeStringField("monkeyType", evt.monkeyType().name());
            gen.writeStringField("eventId", evt.id());
            gen.writeStringField("eventType", evt.eventType().name());
            gen.writeNumberField("eventTime", evt.eventTime().getTime());
            gen.writeStringField("region", evt.region());
            for (Map.Entry<String, String> pair : evt.fields().entrySet()) {
                gen.writeStringField(pair.getKey(), pair.getValue());
            }
            gen.writeEndObject();
        }
        gen.writeEndArray();
        gen.close();
        return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();
    }

    /**
     * POST /api/v1/chaos will try a add a new event with the information in the url context,
     * ignoring the monkey probability and max termination configurations, for a specific instance group.
     *
     * @param content
     *            the Json content passed to the http POST request
     * @return the response
     * @throws IOException
     */
    @POST
    public Response addEvent(String content) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        LOGGER.info(String.format("JSON content: '%s'", content));
        JsonNode input = mapper.readTree(content);

        String eventType = getStringField(input, "eventType");
        String groupType = getStringField(input, "groupType");
        String groupName = getStringField(input, "groupName");
        String chaosTypeName = getStringField(input, "chaosType");

        ChaosType chaosType;
        if (!Strings.isNullOrEmpty(chaosTypeName)) {
            chaosType = ChaosType.parse(this.monkey.getChaosTypes(), chaosTypeName);
        } else {
            chaosType = new ShutdownInstanceChaosType(monkey.context().configuration());
        }

        Response.Status responseStatus;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);
        gen.writeStartObject();
        gen.writeStringField("eventType", eventType);
        gen.writeStringField("groupType", groupType);
        gen.writeStringField("groupName", groupName);
        gen.writeStringField("chaosType", chaosType.getKey());

        if (StringUtils.isEmpty(eventType) || StringUtils.isEmpty(groupType) || StringUtils.isEmpty(groupName)) {
            responseStatus = Response.Status.BAD_REQUEST;
            gen.writeStringField("message", "eventType, groupType, and groupName parameters are all required");
        } else {
            if (eventType.equals("CHAOS_TERMINATION")) {
                responseStatus = addTerminationEvent(groupType, groupName, chaosType, gen);
            } else {
                responseStatus = Response.Status.BAD_REQUEST;
                gen.writeStringField("message", String.format("Unrecognized event type: %s", eventType));
            }
        }
        gen.writeEndObject();
        gen.close();
        LOGGER.info("entity content is '{}'", baos.toString("UTF-8"));
        return Response.status(responseStatus).entity(baos.toString("UTF-8")).build();
    }

    private Response.Status addTerminationEvent(String groupType,
            String groupName, ChaosType chaosType, JsonGenerator gen)
            throws IOException {
        LOGGER.info("Running on-demand termination for instance group type '{}' and name '{}'",
                groupType, groupName);
        Response.Status responseStatus;
        try {
            Event evt = monkey.terminateNow(groupType, groupName, chaosType);
            if (evt != null) {
                responseStatus = Response.Status.OK;
                gen.writeStringField("monkeyType", evt.monkeyType().name());
                gen.writeStringField("eventId", evt.id());
                gen.writeNumberField("eventTime", evt.eventTime().getTime());
                gen.writeStringField("region", evt.region());
                for (Map.Entry<String, String> pair : evt.fields().entrySet()) {
                    gen.writeStringField(pair.getKey(), pair.getValue());
                }
            } else {
                responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
                gen.writeStringField("message",
                        String.format("Failed to terminate instance in group %s [type %s]",
                                groupName, groupType));
            }
        } catch (FeatureNotEnabledException e) {
            responseStatus = Response.Status.FORBIDDEN;
            gen.writeStringField("message", e.getMessage());
        } catch (InstanceGroupNotFoundException e) {
            responseStatus = Response.Status.NOT_FOUND;
            gen.writeStringField("message", e.getMessage());
        } catch (NotFoundException e) {
            // Available instance cannot be found to terminate, maybe the instance is already gone
            responseStatus = Response.Status.GONE;
            gen.writeStringField("message", e.getMessage());
        }
        LOGGER.info("On-demand termination completed.");
        return responseStatus;
    }

    private String getStringField(JsonNode input, String field) {
        JsonNode node = input.get(field);
        if (node == null) {
            return null;
        }
        return node.getTextValue();
    }

}

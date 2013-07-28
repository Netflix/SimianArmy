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
package com.netflix.simianarmy.resources.janitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.janitor.JanitorMonkey;

/**
 * The Class JanitorMonkeyResource for json REST apis.
 */
@Path("/v1/janitor")
public class JanitorMonkeyResource {

    /** The Constant JSON_FACTORY. */
    private static final MappingJsonFactory JSON_FACTORY = new MappingJsonFactory();

    /** The monkey. */
    private final JanitorMonkey monkey;

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JanitorMonkeyResource.class);

    /**
     * Instantiates a janitor monkey resource with a specific janitor monkey.
     *
     * @param monkey
     *          the janitor monkey
     */
    public JanitorMonkeyResource(JanitorMonkey monkey) {
        this.monkey = monkey;
    }

    /**
     * Instantiates a janitor monkey resource using a registered janitor monkey from factory.
     */
    public JanitorMonkeyResource() {
        this.monkey = MonkeyRunner.getInstance().factory(JanitorMonkey.class);
    }

    /**
     * POST /api/v1/janitor will try a add a new event with the information in the url context.
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
        String resourceId = getStringField(input, "resourceId");

        Response.Status responseStatus;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);
        gen.writeStartObject();
        gen.writeStringField("eventType", eventType);
        gen.writeStringField("resourceId", resourceId);

        if (StringUtils.isEmpty(eventType) || StringUtils.isEmpty(resourceId)) {
            responseStatus = Response.Status.BAD_REQUEST;
            gen.writeStringField("message", "eventType and resourceId parameters are all required");
        } else {
            if (eventType.equals("OPTIN")) {
                responseStatus = optInResource(resourceId, true, gen);
            } else if (eventType.equals("OPTOUT")) {
                responseStatus = optInResource(resourceId, false, gen);
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

    private Response.Status optInResource(String resourceId, boolean optIn, JsonGenerator gen)
            throws IOException {
        String op = optIn ? "in" : "out";
        LOGGER.info(String.format("Opt %s resource %s for Janitor Monkey.", op, resourceId));
        Response.Status responseStatus;
        Event evt;
        if (optIn) {
            evt = monkey.optInResource(resourceId);
        } else {
            evt = monkey.optOutResource(resourceId);
        }
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
                    String.format("Failed to opt %s resource %s", op, resourceId));
        }
        LOGGER.info(String.format("Opt %s operation completed.", op));
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

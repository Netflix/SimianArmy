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

import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.janitor.JanitorMonkey;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

/**
 * The Class JanitorMonkeyResource for json REST apis.
 */
@Path("/v1/janitor")
public class JanitorMonkeyResource {

    /** The Constant JSON_FACTORY. */
    private static final MappingJsonFactory JSON_FACTORY = new MappingJsonFactory();

    /** The monkey. */
    private static JanitorMonkey monkey;

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JanitorMonkeyResource.class);

    /**
     * Instantiates a janitor monkey resource with a specific janitor monkey.
     *
     * @param monkey
     *          the janitor monkey
     */
    public JanitorMonkeyResource(JanitorMonkey monkey) {
        JanitorMonkeyResource.monkey = monkey;
    }

    public JanitorMonkeyResource() {
    }

    public JanitorMonkey getJanitorMonkey() {
        if (JanitorMonkeyResource.monkey == null ) {
            JanitorMonkeyResource.monkey = MonkeyRunner.getInstance().factory(JanitorMonkey.class);
        }
        return monkey;
    }

    /**
     * GET /api/v1/janitor/addEvent will try to a add a new event with the information in the url query string.
     * This is the same as the regular POST addEvent except through a query string. This technically isn't
     * very REST-ful as it is a GET call that creates an Opt-out/in event, but is a convenience method
     * for exposing opt-in/opt-out functionality more directly, for example in an email notification.
     *
     * @param eventType eventType from the query string
     * @param resourceId resourceId from the query string
     * @return the response
     * @throws IOException
     */
    @GET @Path("addEvent")
    public Response addEventThroughHttpGet( @QueryParam("eventType") String eventType,  @QueryParam("resourceId") String resourceId,  @QueryParam("region") String region) throws IOException {
        Response.Status responseStatus;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("<html><body style=\"text-align:center\"><img src=\"https://raw.githubusercontent.com/Netflix/SimianArmy/master/assets/janitor.png\" height=\"300\" width=\"300\"><br/>".getBytes());
        if (StringUtils.isEmpty(eventType) || StringUtils.isEmpty(resourceId)) {
            responseStatus = Response.Status.BAD_REQUEST;
            baos.write("<p>NOPE!<br/><br/>Janitor didn't get that: eventType and resourceId parameters are both required</p>".getBytes());
        } else {
            ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
            JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos2, JsonEncoding.UTF8);
            gen.writeStartObject();
            gen.writeStringField("eventType", eventType);
            gen.writeStringField("resourceId", resourceId);

        	if (eventType.equals("OPTIN")) {
                responseStatus = optInResource(resourceId, true, region, gen);
            } else if (eventType.equals("OPTOUT")) {
                responseStatus = optInResource(resourceId, false, region, gen);
            } else {
                responseStatus = Response.Status.BAD_REQUEST;
                gen.writeStringField("message", String.format("Unrecognized event type: %s", eventType));
            }
            gen.writeEndObject();
            gen.close();

        	if(responseStatus == Response.Status.OK) {
        		baos.write(("<p>SUCCESS!<br/><br/>Resource <strong>" + resourceId + "</strong> has been " + eventType + " of Janitor Monkey!</p>").getBytes());
        	} else {
        		baos.write(("<p>NOPE!<br/><br/>Janitor is Confused! Error processing Resource <strong>" + resourceId + "</strong></p>").getBytes());
        	}

        	String jsonout = String.format("<p><em>Monkey JSON Response:</em><br/><br/><textarea cols=40 rows=20>%s</textarea></p>", baos2.toString());
   	    	baos.write(jsonout.getBytes());

        }
    	baos.write("</body></html>".getBytes());
        return Response.status(responseStatus).entity(baos.toString("UTF-8")).build();
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
        String region = getStringField(input, "region");

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
                responseStatus = optInResource(resourceId, true, region, gen);
            } else if (eventType.equals("OPTOUT")) {
                responseStatus = optInResource(resourceId, false, region, gen);
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

    /**
     * Gets the janitor status (e.g. to support an AWS ELB Healthcheck on an instance running JanitorMonkey).
     * Creates GET /api/v1/janitor api which responds 200 OK if JanitorMonkey is running.
     *
     * @param uriInfo
     *            the uri info
     * @return the chaos events json response
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @GET
    public Response getJanitorStatus(@Context UriInfo uriInfo) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);
        gen.writeStartArray();
	gen.writeStartObject();
	gen.writeStringField("JanitorMonkeyStatus", "OnLikeDonkeyKong");
	gen.writeEndObject();
        gen.writeEndArray();
        gen.close();
        return Response.status(Response.Status.OK).entity(baos.toString("UTF-8")).build();
    }

    private Response.Status optInResource(String resourceId, boolean optIn, String region, JsonGenerator gen)
            throws IOException {
        String op = optIn ? "in" : "out";
        LOGGER.info(String.format("Opt %s resource %s for Janitor Monkey.", op, resourceId));
        Response.Status responseStatus;
        Event evt;
        if (optIn) {
            evt = getJanitorMonkey().optInResource(resourceId, region);
        } else {
            evt = getJanitorMonkey().optOutResource(resourceId, region);
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

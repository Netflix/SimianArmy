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

import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.MonkeyRecorder.Event;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Date;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;

@Path("/chaos")
@SuppressWarnings("serial")
public class ChaosMonkeyResource {
    private static final MappingJsonFactory JSON_FACTORY = new MappingJsonFactory();

    private ChaosMonkey monkey = MonkeyRunner.getInstance().factory(ChaosMonkey.class);

    @GET
    public Response getChaosEvents(@javax.ws.rs.core.Context UriInfo uriInfo) throws IOException {
        Map<String, String> query = new HashMap<String, String>();
        Date date = new Date(0);
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

        List<Event> evts = monkey.context().recorder()
                .findEvents(ChaosMonkey.Type.CHAOS, ChaosMonkey.EventTypes.CHAOS_TERMINATION, query, date);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator gen = JSON_FACTORY.createJsonGenerator(baos, JsonEncoding.UTF8);
        gen.writeStartArray();
        for (Event evt : evts) {
            gen.writeStartObject();
            gen.writeStringField("monkeyType", evt.monkeyType().name());
            gen.writeStringField("eventType", evt.eventType().name());
            gen.writeNumberField("eventTime", evt.eventTime().getTime());
            for (Map.Entry<String, String> pair : evt.fields().entrySet()) {
                gen.writeStringField(pair.getKey(), pair.getValue());
            }
            gen.writeEndObject();
        }
        gen.writeEndArray();
        gen.close();
        return Response.status(Response.Status.OK).entity(baos).build();
    }
}

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
package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyConfiguration;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyRunner;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;

public class ChaosMonkey extends Monkey {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChaosMonkey.class);
    private static final String NS = "simianarmy.chaos.";

    public interface Context extends Monkey.Context {
        MonkeyConfiguration configuration();

        ChaosCrawler chaosCrawler();

        ChaosInstanceSelector chaosInstanceSelector();
    }

    private Context ctx;
    private MonkeyConfiguration cfg;
    private long runsPerDay;

    public ChaosMonkey(Context ctx) {
        super(ctx);
        this.ctx = ctx;
        this.cfg = ctx.configuration();

        Calendar open = ctx.calendar().now();
        Calendar close = ctx.calendar().now();
        open.set(Calendar.HOUR, ctx.calendar().openHour());
        close.set(Calendar.HOUR, ctx.calendar().closeHour());

        TimeUnit freqUnit = ctx.scheduler().frequencyUnit();
        long units = freqUnit.convert(close.getTimeInMillis() - open.getTimeInMillis(), TimeUnit.MILLISECONDS);
        runsPerDay = units / ctx.scheduler().frequency();
    }

    public enum Type {
        CHAOS
    }

    public Enum type() {
        return Type.CHAOS;
    }

    public enum EventTypes {
        CHAOS_TERMINATION
    }

    public void doMonkeyBusiness() {
        cfg.reload();
        String prop = NS + "enabled";
        if (!cfg.getBoolOrElse(prop, true)) {
            LOGGER.info("ChaosMonkey disabled, set {}=true", prop);
            return;
        }

        for (InstanceGroup group : ctx.chaosCrawler().groups()) {
            prop = NS + group.type() + "." + group.name() + ".enabled";
            String defaultProp = NS + group.type();
            if (cfg.getBoolOrElse(prop, cfg.getBool(defaultProp + ".enabled"))) {
                String probProp = NS + group.type() + "." + group.name() + ".probability";
                double prob = cfg.getNumOrElse(probProp, cfg.getNumOrElse(defaultProp + ".probability", 1.0));
                String inst = ctx.chaosInstanceSelector().select(group, prob / runsPerDay);
                if (inst != null) {
                    prop = NS + "leashed";
                    if (cfg.getBoolOrElse(prop, true)) {
                        LOGGER.info("leashed ChaosMonkey prevented from killing {}, set {}=false", inst, prop);
                    } else {
                        if (hasPreviousTerminations(group)) {
                            LOGGER.info("ChaosMonkey takes pity on group {} [{}] since it was attacked ealier today",
                                    group.name(), group.type());
                            continue;
                        }
                        try {
                            recordTermination(group, inst);
                            ctx.cloudClient().terminateInstance(inst);
                        }
                        catch ( Exception e ) {
                            handleTerminationError(inst, e);
                        }
                    }
                }
            } else {
                LOGGER.info("Group {} [type {}] disabled, set {}=true or {}=true",
                        new Object[] {group.name(), group.type(), prop, defaultProp + ".enabled"});
            }
        }
    }

    // abstracted so subclasses can decide to continue causing chaos if desired
    protected void handleTerminationError(String instance, Throwable e) {
        LOGGER.error("failed to terminate instance " + instance, e.getMessage());
        throw e;
    }

    protected boolean hasPreviousTerminations(InstanceGroup group) {
        Map<String, String> query = new HashMap<String, String>();
        query.put("groupType", group.type().name());
        query.put("groupName", group.name());
        Calendar today = Calendar.getInstance();
        // set to midnight
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        List<Event> evts = ctx.recorder().findEvents(Type.CHAOS, EventTypes.CHAOS_TERMINATION, query, today.getTime());
        return !evts.isEmpty();
    }

    protected void recordTermination(InstanceGroup group, String instance) {
        Event evt = ctx.recorder().newEvent(Type.CHAOS, EventTypes.CHAOS_TERMINATION, instance);
        evt.addField("groupType", group.type().name());
        evt.addField("groupName", group.name());
        ctx.recorder().recordEvent(evt);
    }

    @Path("/chaos")
    @SuppressWarnings("serial")
    public static class Servlet {
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

            List<Event> evts = monkey.ctx.recorder().findEvents(Type.CHAOS, EventTypes.CHAOS_TERMINATION, query, date);

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
}

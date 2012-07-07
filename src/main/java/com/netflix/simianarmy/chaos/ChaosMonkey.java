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
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

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
                        recordTermination(group, inst);
                        ctx.cloudClient().terminateInstance(inst);
                    }
                }
            } else {
                LOGGER.info("Group {} [type {}] disabled, set {}=true or {}=true",
                        new Object[] {group.name(), group.type(), prop, defaultProp + ".enabled"});
            }
        }
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
}

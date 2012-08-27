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

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.NotFoundException;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosMonkey;

/**
 * The Class BasicChaosMonkey.
 */
public class BasicChaosMonkey extends ChaosMonkey {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicChaosMonkey.class);

    /** The Constant NS. */
    private static final String NS = "simianarmy.chaos.";

    /** The cfg. */
    private MonkeyConfiguration cfg;

    /** The runs per day. */
    private long runsPerDay;

    /**
     * Instantiates a new basic chaos monkey.
     *
     * @param ctx
     *            the ctx
     */
    public BasicChaosMonkey(ChaosMonkey.Context ctx) {
        super(ctx);

        this.cfg = ctx.configuration();

        Calendar open = ctx.calendar().now();
        Calendar close = ctx.calendar().now();
        open.set(Calendar.HOUR, ctx.calendar().openHour());
        close.set(Calendar.HOUR, ctx.calendar().closeHour());

        TimeUnit freqUnit = ctx.scheduler().frequencyUnit();
        long units = freqUnit.convert(close.getTimeInMillis() - open.getTimeInMillis(), TimeUnit.MILLISECONDS);
        runsPerDay = units / ctx.scheduler().frequency();
    }

    /** {@inheritDoc} */
    public void doMonkeyBusiness() {
        context().resetEventReport();
        cfg.reload();
        String prop = NS + "enabled";
        if (!cfg.getBoolOrElse(prop, true)) {
            LOGGER.info("ChaosMonkey disabled, set {}=true", prop);
            return;
        }

        for (InstanceGroup group : context().chaosCrawler().groups()) {
            prop = NS + group.type() + "." + group.name() + ".enabled";
            String defaultProp = NS + group.type();
            boolean isGroupTypeEnabled = cfg.getBool(defaultProp + ".enabled");
            boolean isGroupNameEnabled = cfg.getBoolOrElse(prop, isGroupTypeEnabled);
            if (isGroupNameEnabled) {
                String probPropability = NS + group.type() + "." + group.name() + ".probability";
                double probability = cfg.getNumOrElse(probPropability, cfg.getNumOrElse(defaultProp + ".probability", 1.0));
                LOGGER.info("Group {} [type {}] enabled [prob {}]", new Object[] {group.name(), group.type(), probability});
                String instanceId = context().chaosInstanceSelector().select(group, probability / runsPerDay);
                if (instanceId != null) {
                    prop = NS + "leashed";
                    if (cfg.getBoolOrElse(prop, true)) {
                        LOGGER.info("leashed ChaosMonkey prevented from killing {} from group {} [{}], set {}=false",
                                new Object[] {instanceId, group.name(), group.type(), prop});
                        reportEventForSummary(EventTypes.CHAOS_TERMINATION_SKIPPED, group, instanceId);
                    } else {
                        if (hasPreviousTerminations(group)) {
                            LOGGER.info("ChaosMonkey takes pity on group {} [{}] since it was attacked ealier today",
                                    group.name(), group.type());
                            reportEventForSummary(EventTypes.CHAOS_TERMINATION_SKIPPED, group, instanceId);
                            continue;
                        }
                        try {
                            context().cloudClient().terminateInstance(instanceId);
                            recordTermination(group, instanceId);
                            reportEventForSummary(EventTypes.CHAOS_TERMINATION, group, instanceId);
                            LOGGER.info("Terminated {} from group {} [{}]",
                                    new Object[] {instanceId, group.name(), group.type()});
                        } catch (NotFoundException e) {
                            LOGGER.warn("Failed to terminate " + instanceId
                                    + ", it does not exist. Perhaps it was already terminated");
                            reportEventForSummary(EventTypes.CHAOS_TERMINATION_SKIPPED, group, instanceId);
                        } catch (Exception e) {
                            reportEventForSummary(EventTypes.CHAOS_TERMINATION_SKIPPED, group, instanceId);
                            handleTerminationError(instanceId, e);
                        }
                    }
                }
            } else {
                LOGGER.info("Group {} [type {}] disabled, set {}=true or {}=true",
                        new Object[] {group.name(), group.type(), prop, defaultProp + ".enabled"});
            }
        }
    }

    private void reportEventForSummary(EventTypes eventType, InstanceGroup group, String instanceId) {
        context().reportEvent(createEvent(eventType, group, instanceId));        
    }

    /**
     * Handle termination error. This has been abstracted so subclasses can decide to continue causing chaos if desired.
     *
     * @param instance
     *            the instance
     * @param e
     *            the exception
     */
    protected void handleTerminationError(String instance, Throwable e) {
        LOGGER.error("failed to terminate instance " + instance, e.getMessage());
        throw new RuntimeException("failed to terminate instance " + instance, e);
    }

    /** {@inheritDoc} */
    public boolean hasPreviousTerminations(InstanceGroup group) {
        Map<String, String> query = new HashMap<String, String>();
        query.put("groupType", group.type().name());
        query.put("groupName", group.name());
        Calendar today = Calendar.getInstance();
        // set to midnight
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        List<Event> evts = context().recorder().findEvents(Type.CHAOS, EventTypes.CHAOS_TERMINATION, query,
                today.getTime());
        return !evts.isEmpty();
    }

    /** {@inheritDoc} */
    public void recordTermination(InstanceGroup group, String instance) {
        Event evt = createEvent(EventTypes.CHAOS_TERMINATION, group, instance);
        context().recorder().recordEvent(evt);
    }

    private Event createEvent(EventTypes chaosTermination, InstanceGroup group, String instance) {
        Event evt = context().recorder().newEvent(Type.CHAOS, chaosTermination, group.region(), instance);
        evt.addField("groupType", group.type().name());
        evt.addField("groupName", group.name());
        return evt;
    }
}

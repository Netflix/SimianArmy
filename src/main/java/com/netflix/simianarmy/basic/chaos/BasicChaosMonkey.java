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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.FeatureNotEnabledException;
import com.netflix.simianarmy.InstanceGroupNotFoundException;
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
    private final MonkeyConfiguration cfg;

    /** The runs per day. */
    private final long runsPerDay;

    /** The minimum value of the maxTerminationCountPerday property to be considered non-zero. **/
    private static final double MIN_MAX_TERMINATION_COUNT_PER_DAY = 0.001;

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
    @Override
    public void doMonkeyBusiness() {
        cfg.reload();
        if (!isEnabled()) {
            return;
        }
        String prop;
        for (InstanceGroup group : context().chaosCrawler().groups()) {
            prop = NS + group.type() + "." + group.name() + ".enabled";
            String defaultProp = NS + group.type();
            if (cfg.getBoolOrElse(prop, cfg.getBool(defaultProp + ".enabled"))) {
                if (isMaxTerminationCountExceeded(group)) {
                    continue;
                }
                String probProp = NS + group.type() + "." + group.name() + ".probability";
                double prob = cfg.getNumOrElse(probProp, cfg.getNumOrElse(defaultProp + ".probability", 1.0));
                LOGGER.info("Group {} [type {}] enabled [prob {}]", new Object[]{group.name(), group.type(), prob});
                String inst = context().chaosInstanceSelector().select(group, prob / runsPerDay);
                if (inst != null) {
                    terminateInstance(group, inst);
                }
            } else {
                LOGGER.info("Group {} [type {}] disabled, set {}=true or {}=true",
                        new Object[]{group.name(), group.type(), prop, defaultProp + ".enabled"});
            }
        }
    }

    @Override
    public Event terminateNow(String type, String name)
            throws FeatureNotEnabledException, InstanceGroupNotFoundException {
        Validate.notNull(type);
        Validate.notNull(name);
        cfg.reload();
        if (!isEnabled()) {
            String msg = String.format("Chaos monkey is not enabled for group %s [type %s]",
                    name, type);
            LOGGER.info(msg);
            throw new FeatureNotEnabledException(msg);
        }
        String prop = NS + "terminateOndemand.enabled";
        if (cfg.getBool(prop)) {
            InstanceGroup group = findInstanceGroup(type, name);
            if (group == null) {
                throw new InstanceGroupNotFoundException(type, name);
            }
            String inst = context().chaosInstanceSelector().select(group, 1.0);
            if (inst != null) {
                return terminateInstance(group, inst);
            } else {
                throw new NotFoundException(String.format("No instance is found in group %s [type %s]",
                        name, type));
            }
        } else {
            String msg = String.format("Group %s [type %s] does not allow on-demand termination, set %s=true",
                    name, type, prop);
            LOGGER.info(msg);
            throw new FeatureNotEnabledException(msg);
        }
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
    @Override
    public Event recordTermination(InstanceGroup group, String instance) {
        Event evt = context().recorder().newEvent(Type.CHAOS, EventTypes.CHAOS_TERMINATION, group.region(), instance);
        evt.addField("groupType", group.type().name());
        evt.addField("groupName", group.name());
        context().recorder().recordEvent(evt);
        return evt;
    }

    /** {@inheritDoc} */
    @Override
    public int getPreviousTerminationCount(InstanceGroup group, Date after) {
        Map<String, String> query = new HashMap<String, String>();
        query.put("groupType", group.type().name());
        query.put("groupName", group.name());
        List<Event> evts = context().recorder().findEvents(Type.CHAOS, EventTypes.CHAOS_TERMINATION, query, after);
        return evts.size();
    }

    private boolean isEnabled() {
        String prop = NS + "enabled";
        if (cfg.getBoolOrElse(prop, true)) {
            return true;
        }
        LOGGER.info("ChaosMonkey disabled, set {}=true", prop);
        return false;
    }

    private InstanceGroup findInstanceGroup(String type, String name) {
        // Calling context().chaosCrawler().groups(name) causes a new crawl to get
        // the up to date information for the group name.
        for (InstanceGroup group : context().chaosCrawler().groups(name)) {
            if (group.type().toString().equals(type) && group.name().equals(name)) {
                return group;
            }
        }
        LOGGER.warn("Failed to find instance group for type {} and name {}", type, name);
        return null;
    }

    private Event terminateInstance(InstanceGroup group, String inst) {
        Validate.notNull(group);
        Validate.notEmpty(inst);
        String prop = NS + "leashed";
        if (cfg.getBoolOrElse(prop, true)) {
            LOGGER.info("leashed ChaosMonkey prevented from killing {} from group {} [{}], set {}=false",
                    new Object[]{inst, group.name(), group.type(), prop});
            return null;
        } else {
            try {
                Event evt = recordTermination(group, inst);
                context().cloudClient().terminateInstance(inst);
                LOGGER.info("Terminated {} from group {} [{}]", new Object[]{inst, group.name(), group.type()});
                return evt;
            } catch (NotFoundException e) {
                LOGGER.warn("Failed to terminate " + inst + ", it does not exist. Perhaps it was already terminated");
                return null;
            } catch (Exception e) {
                handleTerminationError(inst, e);
                return null;
            }
        }
    }

    private boolean isMaxTerminationCountExceeded(InstanceGroup group) {
        Validate.notNull(group);
        String propName = "maxTerminationsPerDay";
        String defaultProp = String.format("%s%s.%s", NS, group.type(), propName);
        String prop = String.format("%s%s.%s.%s", NS, group.type(), group.name(), propName);
        double maxTerminationsPerDay = cfg.getNumOrElse(prop, cfg.getNumOrElse(defaultProp, 1.0));
        if (maxTerminationsPerDay <= MIN_MAX_TERMINATION_COUNT_PER_DAY) {
            LOGGER.info("ChaosMonkey is configured to not allow any killing from group {} [{}] "
                    + "with max daily count set as {}", new Object[]{group.name(), group.type(), prop});
            return true;
        } else {
            int daysBack = 1;
            int maxCount = (int) maxTerminationsPerDay;
            if (maxTerminationsPerDay < 1.0) {
                daysBack = (int) Math.ceil(1 / maxTerminationsPerDay);
                maxCount = 1;
            }
            Calendar after = Calendar.getInstance();
            after.add(Calendar.DATE, -1 * daysBack);
            // Check if the group has exceeded the maximum terminations for the last period
            int terminationCount = getPreviousTerminationCount(group, after.getTime());
            if (terminationCount >= maxCount) {
                LOGGER.info("The count of terminations in the last {} days is {}, equal or greater than"
                        + " the max count threshold {}", new Object[]{daysBack, terminationCount, maxCount});
                return true;
            }
        }
        return false;
    }
}

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

import com.google.common.collect.Lists;
import com.netflix.simianarmy.*;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.chaos.*;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

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

    private final MonkeyCalendar monkeyCalendar;

    // When a mandatory termination is triggered due to the minimum termination limit is breached,
    // the value below is used as the termination probability.
    private static final double DEFAULT_MANDATORY_TERMINATION_PROBABILITY = 0.5;

    private final List<ChaosType> allChaosTypes;

    /**
     * Instantiates a new basic chaos monkey.
     * @param ctx
     *            the ctx
     */
    public BasicChaosMonkey(ChaosMonkey.Context ctx) {
        super(ctx);

        this.cfg = ctx.configuration();
        this.monkeyCalendar = ctx.calendar();

        Calendar open = monkeyCalendar.now();
        Calendar close = monkeyCalendar.now();
        open.set(Calendar.HOUR, monkeyCalendar.openHour());
        close.set(Calendar.HOUR, monkeyCalendar.closeHour());

        allChaosTypes = Lists.newArrayList();
        allChaosTypes.add(new ShutdownInstanceChaosType(cfg));
        allChaosTypes.add(new BlockAllNetworkTrafficChaosType(cfg));
        allChaosTypes.add(new DetachVolumesChaosType(cfg));
        allChaosTypes.add(new BurnCpuChaosType(cfg));
        allChaosTypes.add(new BurnIoChaosType(cfg));
        allChaosTypes.add(new KillProcessesChaosType(cfg));
        allChaosTypes.add(new NullRouteChaosType(cfg));
        allChaosTypes.add(new FailEc2ChaosType(cfg));
        allChaosTypes.add(new FailDnsChaosType(cfg));
        allChaosTypes.add(new FailDynamoDbChaosType(cfg));
        allChaosTypes.add(new FailS3ChaosType(cfg));
        allChaosTypes.add(new FillDiskChaosType(cfg));
        allChaosTypes.add(new NetworkCorruptionChaosType(cfg));
        allChaosTypes.add(new NetworkLatencyChaosType(cfg));
        allChaosTypes.add(new NetworkLossChaosType(cfg));

        TimeUnit freqUnit = ctx.scheduler().frequencyUnit();
        if (TimeUnit.DAYS == freqUnit) {
            runsPerDay = ctx.scheduler().frequency();
        } else {
            long units = freqUnit.convert(close.getTimeInMillis() - open.getTimeInMillis(), TimeUnit.MILLISECONDS);
            runsPerDay = units / ctx.scheduler().frequency();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void doMonkeyBusiness() {
            context().resetEventReport();
            cfg.reload();
            if (!isChaosMonkeyEnabled()) {
                return;
            }
            for (InstanceGroup group : context().chaosCrawler().groups()) {
                if (isGroupEnabled(group)) {
                    if (isMaxTerminationCountExceeded(group)) {
                        continue;
                    }
                    double prob = getEffectiveProbability(group);
                    Collection<String> instances = context().chaosInstanceSelector().select(group, prob / runsPerDay);
                    for (String inst : instances) {
                        if (isMaxTerminationCountExceeded(group)) {
                            break;
                        }
                        ChaosType chaosType = pickChaosType(context().cloudClient(), inst);
                        if (chaosType == null) {
                            // This is surprising ... normally we can always just terminate it
                            LOGGER.warn("No chaos type was applicable to the instance: {}", inst);
                            continue;
                        }
                        terminateInstance(group, inst, chaosType);
                    }
                }
            }
    }

    private ChaosType pickChaosType(CloudClient cloudClient, String instanceId) {
        Random random = new Random();

        SshConfig sshConfig = new SshConfig(cfg);
        ChaosInstance instance = new ChaosInstance(cloudClient, instanceId, sshConfig);

        List<ChaosType> applicable = Lists.newArrayList();
        for (ChaosType chaosType : allChaosTypes) {
            if (chaosType.isEnabled() && chaosType.canApply(instance)) {
                applicable.add(chaosType);
            }
        }

        if (applicable.isEmpty()) {
            return null;
        }

        int index = random.nextInt(applicable.size());
        return applicable.get(index);
    }

    @Override
    public Event terminateNow(String type, String name, ChaosType chaosType)
            throws FeatureNotEnabledException, InstanceGroupNotFoundException {
        Validate.notNull(type);
        Validate.notNull(name);
        cfg.reload(name);
        if (!isChaosMonkeyEnabled()) {
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
            Collection<String> instances = context().chaosInstanceSelector().select(group, 1.0);
            Validate.isTrue(instances.size() <= 1);
            if (instances.size() == 1) {
                return terminateInstance(group, instances.iterator().next(), chaosType);
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
        LOGGER.error("failed to terminate instance " + instance, e);
        throw new RuntimeException("failed to terminate instance " + instance, e);
    }

    /** {@inheritDoc} */
    @Override
    public Event recordTermination(InstanceGroup group, String instance, ChaosType chaosType) {
        Event evt = context().recorder().newEvent(Type.CHAOS, EventTypes.CHAOS_TERMINATION, group.region(), instance);
        evt.addField("groupType", group.type().name());
        evt.addField("groupName", group.name());
        evt.addField("chaosType", chaosType.getKey());
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

    private Event createEvent(EventTypes chaosTermination, InstanceGroup group, String instance) {
        Event evt = context().recorder().newEvent(Type.CHAOS, chaosTermination, group.region(), instance);
        evt.addField("groupType", group.type().name());
        evt.addField("groupName", group.name());
        return evt;
    }

    /**
     * Gets the effective probability value, returns 0 if the group is not enabled. Otherwise calls
     * getEffectiveProbability.
     * @param group
     * @return the effective probability value for the instance group
     */
    protected double getEffectiveProbability(InstanceGroup group) {
        if (!isGroupEnabled(group)) {
            return 0;
        }
        return getEffectiveProbabilityFromCfg(group);
    }

    /**
     * Gets the effective probability value when the monkey processes an instance group, it uses the following
     * logic in the order as listed below.
     *
     * 1) When minimum mandatory termination is enabled, a default non-zero probability is used for opted-in
     * groups, if a) the application has been opted in for the last mandatory termination window
     *        and b) there was no terminations in the last mandatory termination window
     * 2) Use the probability configured for the group type and name
     * 3) Use the probability configured for the group
     * 4) Use 1.0
     * @param group
     * @return double
     */
    protected double getEffectiveProbabilityFromCfg(InstanceGroup group) {
        String propName;
        if (cfg.getBool(NS + "mandatoryTermination.enabled")) {
            String mtwProp = NS + "mandatoryTermination.windowInDays";
            int mandatoryTerminationWindowInDays = (int) cfg.getNumOrElse(mtwProp, 0);
            if (mandatoryTerminationWindowInDays > 0
                    && noTerminationInLastWindow(group, mandatoryTerminationWindowInDays)) {
                double mandatoryProb = cfg.getNumOrElse(NS + "mandatoryTermination.defaultProbability",
                        DEFAULT_MANDATORY_TERMINATION_PROBABILITY);
                LOGGER.info("There has been no terminations for group {} [type {}] in the last {} days,"
                        + "setting the probability to {} for mandatory termination.",
                        new Object[]{group.name(), group.type(), mandatoryTerminationWindowInDays, mandatoryProb});
                return mandatoryProb;
            }
        }
        propName = "probability";
        double prob = getNumFromCfgOrDefault(group, propName, 1.0);
        LOGGER.info("Group {} [type {}] enabled [prob {}]", new Object[]{group.name(), group.type(), prob});
        return prob;
    }

    protected double getNumFromCfgOrDefault(InstanceGroup group, String propName, double defaultValue) {
        String defaultProp = String.format("%s%s.%s", NS, group.type(), propName);
        String prop = String.format("%s%s.%s.%s", NS, group.type(), group.name(), propName);
        return cfg.getNumOrElse(prop, cfg.getNumOrElse(defaultProp, defaultValue));
    }

    protected boolean getBoolFromCfgOrDefault(InstanceGroup group, String propName, boolean defaultValue) {
        String defaultProp = String.format("%s%s.%s", NS, group.type(), propName);
        String prop = String.format("%s%s.%s.%s", NS, group.type(), group.name(), propName);
        return cfg.getBoolOrElse(prop, cfg.getBoolOrElse(defaultProp, defaultValue));
    }

    /**
     * Returns lastOptInTimeInMilliseconds from the .properties file.
     *
     * @param group
     * @return long
     */
    protected long getLastOptInMilliseconds(InstanceGroup group) {
        String prop = NS + group.type() + "." + group.name() + ".lastOptInTimeInMilliseconds";
        long lastOptInTimeInMilliseconds = (long) cfg.getNumOrElse(prop, -1);
        return lastOptInTimeInMilliseconds;
    }

    private boolean noTerminationInLastWindow(InstanceGroup group, int mandatoryTerminationWindowInDays) {
    long lastOptInTimeInMilliseconds = getLastOptInMilliseconds(group);
        if (lastOptInTimeInMilliseconds < 0) {
            return false;
        }

        Calendar windowStart = monkeyCalendar.now();
        windowStart.add(Calendar.DATE, -1 * mandatoryTerminationWindowInDays);

        // return true if the window start is after the last opt-in time and
        // there has been no termination since the window start
        if (windowStart.getTimeInMillis() > lastOptInTimeInMilliseconds
                && getPreviousTerminationCount(group, windowStart.getTime()) <= 0) {
            return true;
        }

        return false;
    }

    /**
     * Checks to see if the given instance group is enabled.
     * @param group
     * @return boolean
     */
    protected boolean isGroupEnabled(InstanceGroup group) {
        boolean enabled = getBoolFromCfgOrDefault(group, "enabled", false);
        if (enabled) {
            return true;
        } else {
            String prop = NS + group.type() + "." + group.name() + ".enabled";
            String defaultProp = NS + group.type() + ".enabled";
            LOGGER.info("Group {} [type {}] disabled, set {}=true or {}=true",
                    new Object[]{group.name(), group.type(), prop, defaultProp});
            return false;
        }
    }

    private boolean isChaosMonkeyEnabled() {
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

    protected Event terminateInstance(InstanceGroup group, String inst, ChaosType chaosType) {
        Validate.notNull(group);
        Validate.notEmpty(inst);
        String prop = NS + "leashed";
        if (cfg.getBoolOrElse(prop, true)) {
            LOGGER.info("leashed ChaosMonkey prevented from killing {} from group {} [{}], set {}=false",
                    new Object[]{inst, group.name(), group.type(), prop});
            reportEventForSummary(EventTypes.CHAOS_TERMINATION_SKIPPED, group, inst);
            return null;
        } else {
            try {
                Event evt = recordTermination(group, inst, chaosType);
                sendTerminationNotification(group, inst, chaosType);
                SshConfig sshConfig = new SshConfig(cfg);
                ChaosInstance chaosInstance = new ChaosInstance(context().cloudClient(), inst, sshConfig);
                chaosType.apply(chaosInstance);
                LOGGER.info("Terminated {} from group {} [{}] with {}",
                        new Object[]{inst, group.name(), group.type(), chaosType.getKey() });
                reportEventForSummary(EventTypes.CHAOS_TERMINATION, group, inst);
                return evt;
            } catch (NotFoundException e) {
                LOGGER.warn("Failed to terminate " + inst + ", it does not exist. Perhaps it was already terminated");
                reportEventForSummary(EventTypes.CHAOS_TERMINATION_SKIPPED, group, inst);
                return null;
            } catch (Exception e) {
                handleTerminationError(inst, e);
                reportEventForSummary(EventTypes.CHAOS_TERMINATION_SKIPPED, group, inst);
                return null;
            }
        }
    }

    /**
     * Checks to see if the maximum termination window has been exceeded.
     *
     * @param group
     * @return boolean
     */
    protected boolean isMaxTerminationCountExceeded(InstanceGroup group) {
        Validate.notNull(group);
        String propName = "maxTerminationsPerDay";
        double maxTerminationsPerDay = getNumFromCfgOrDefault(group, propName, 1.0);
        if (maxTerminationsPerDay <= MIN_MAX_TERMINATION_COUNT_PER_DAY) {
            String prop = String.format("%s%s.%s.%s", NS, group.type(), group.name(), propName);
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
            Calendar after = monkeyCalendar.now();
            after.add(Calendar.DATE, -1 * daysBack);
            // Check if the group has exceeded the maximum terminations for the last period
            int terminationCount = getPreviousTerminationCount(group, after.getTime());
            if (terminationCount >= maxCount) {
                LOGGER.info("The count of terminations for group {} [{}] in the last {} days is {},"
                        + " equal or greater than the max count threshold {}",
                        new Object[]{group.name(), group.type(), daysBack, terminationCount, maxCount});
                return true;
            }
        }
        return false;
    }

    @Override
    public void sendTerminationNotification(InstanceGroup group, String instance, ChaosType chaosType) {
        String propEmailGlobalEnabled = "simianarmy.chaos.notification.global.enabled";
        String propEmailGroupEnabled = String.format("%s%s.%s.notification.enabled", NS, group.type(), group.name());

        ChaosEmailNotifier notifier = context().chaosEmailNotifier();
        if (notifier == null) {
            String msg = "Chaos email notifier is not set.";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        if (cfg.getBoolOrElse(propEmailGroupEnabled, false)) {
            notifier.sendTerminationNotification(group, instance, chaosType);
        }
        if (cfg.getBoolOrElse(propEmailGlobalEnabled, false)) {
            notifier.sendTerminationGlobalNotification(group, instance, chaosType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ChaosType> getChaosTypes() {
        return Lists.newArrayList(allChaosTypes);
    }
}
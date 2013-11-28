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

import java.util.Date;
import java.util.List;

import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.FeatureNotEnabledException;
import com.netflix.simianarmy.InstanceGroupNotFoundException;
import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.MonkeyType;

/**
 * The Class ChaosMonkey.
 */
public abstract class ChaosMonkey extends Monkey {

    /**
     * The Interface Context.
     */
    public interface Context extends Monkey.Context {

        /**
         * Configuration.
         *
         * @return the monkey configuration
         */
        MonkeyConfiguration configuration();

        /**
         * Chaos crawler.
         *
         * @return the chaos crawler
         */
        ChaosCrawler chaosCrawler();

        /**
         * Chaos instance selector.
         *
         * @return the chaos instance selector
         */
        ChaosInstanceSelector chaosInstanceSelector();

        /**
         * Chaos email notifier.
         *
         * @return the chaos email notifier
         */
        ChaosEmailNotifier chaosEmailNotifier();
    }

    /** The context. */
    private final Context ctx;

    /**
     * Instantiates a new chaos monkey.
     *
     * @param ctx
     *            the context.
     */
    public ChaosMonkey(Context ctx) {
        super(ctx);
        this.ctx = ctx;
    }

    /**
     * The monkey Type.
     */
    public enum Type implements MonkeyType {

        /** chaos monkey. */
        CHAOS
    }

    /**
     * The event types that this monkey causes.
     */
    public enum EventTypes implements EventType {

        /** The chaos termination. */
        CHAOS_TERMINATION, CHAOS_TERMINATION_SKIPPED
    }

    /** {@inheritDoc} */
    @Override
    public final Type type() {
        return Type.CHAOS;
    }

    /** {@inheritDoc} */
    @Override
    public Context context() {
        return ctx;
    }

    /** {@inheritDoc} */
    @Override
    public abstract void doMonkeyBusiness();

    /**
     * Gets the count of terminations since a specific time. Chaos should probably not continue to beat up an instance
     * group if the count exceeds a threshold.
     *
     * @param group
     *            the group
     * @return true, if successful
     */
    public abstract int getPreviousTerminationCount(ChaosCrawler.InstanceGroup group, Date after);

    /**
     * Record termination. This is used to notify system owners of terminations and to record terminations so that Chaos
     * does not continue to thrash the instance groups on later runs.
     *
     * @param group
     *            the group
     * @param instance
     *            the instance
     * @return the termination event
     */
    public abstract Event recordTermination(ChaosCrawler.InstanceGroup group, String instance, ChaosType chaosType);

    /**
     * Terminates one instance right away from an instance group when there are available instances.
     * @param type
     *            the type of the instance group
     * @param name
     *            the name of the instance group
     * @return the termination event
     * @throws FeatureNotEnabledException
     * @throws InstanceGroupNotFoundException
     */
    public abstract Event terminateNow(String type, String name, ChaosType chaosType)
            throws FeatureNotEnabledException, InstanceGroupNotFoundException;

    /**
     * Sends notification for the termination to the instance owners.
     *
     * @param group
     *            the group
     * @param instance
     *            the instance
     * @param chaosType
     *            the chaos monkey strategy that was chosen
     */
    public abstract void sendTerminationNotification(ChaosCrawler.InstanceGroup group, String instance,
            ChaosType chaosType);

    /**
     * Gets a list of all enabled chaos types for this ChaosMonkey.
     */
    public abstract List<ChaosType> getChaosTypes();
}

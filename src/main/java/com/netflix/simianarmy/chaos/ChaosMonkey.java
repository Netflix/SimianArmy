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

import com.netflix.simianarmy.FeatureNotEnabledException;
import com.netflix.simianarmy.InstanceGroupNotFoundException;
import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder.Event;

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
    public enum Type {

        /** chaos monkey. */
        CHAOS
    }

    /**
     * The event types that this monkey causes.
     */
    public enum EventTypes {

        /** The chaos termination. */
        CHAOS_TERMINATION
    }

    /** {@inheritDoc} */
    @Override
    public final Enum type() {
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
    public abstract Event recordTermination(ChaosCrawler.InstanceGroup group, String instance);

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
    public abstract Event terminateNow(String type, String name)
            throws FeatureNotEnabledException, InstanceGroupNotFoundException;
}

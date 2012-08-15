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

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyConfiguration;

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
     * Checks for previous terminations for today from the midnight. Chaos should probably not continue to beat
     * up an instance group if it has already been thrashed today.
     *
     * @param group
     *            the group
     * @return true, if successful
     * @deprecated Should use getPreviousTerminationCount
     */
    @Deprecated
    public abstract boolean hasPreviousTerminations(ChaosCrawler.InstanceGroup group);

    /**
     * Gets the count of terminations since a specific time. Chaos should probably not continue to beat up an
     * instance group if the count exceeds a threshold.
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
     */
    public abstract void recordTermination(ChaosCrawler.InstanceGroup group, String instance);
}

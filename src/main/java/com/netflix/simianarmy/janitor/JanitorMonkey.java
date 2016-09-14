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
package com.netflix.simianarmy.janitor;

import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.MonkeyType;

import java.util.List;

/**
 * The abstract class for a Janitor Monkey.
 */
public abstract class JanitorMonkey extends Monkey {

    /**  The key name of the Janitor tag used to tag resources. */
    public static final String JANITOR_TAG = "janitor";
    /** The key name of the Janitor meta tag used to tag resources. */
    public static final String JANITOR_META_TAG = "JANITOR_META";
    /** The key name of the tag instance used to tag resources. */
    public static final String INSTANCE_TAG_KEY = "instance";
    /** The key name of the tag detach time used to tag resources. */
    public static final String DETACH_TIME_TAG_KEY = "detachTime";

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
         * Janitors run by this monkey.
         * @return the janitors
         */
        List<AbstractJanitor> janitors();

        /**
         * Email notifier used to send notifications by the janitor monkey.
         * @return the email notifier
         */
        JanitorEmailNotifier emailNotifier();

        /**
         * The region the monkey is running in.
         * @return the region the monkey is running in.
         */
        String region();

        /**
         * The accountName the monkey is running in.
         * @return the accountName the monkey is running in.
         */
        String accountName();

        /**
         * The Janitor resource tracker.
         * @return the Janitor resource tracker.
         */
        JanitorResourceTracker resourceTracker();
    }

    /** The context. */
    private final Context ctx;

    /**
     * Instantiates a new janitor monkey.
     *
     * @param ctx
     *            the context.
     */
    public JanitorMonkey(Context ctx) {
        super(ctx);
        this.ctx = ctx;
    }

    /**
     * The monkey Type.
     */
    public static enum Type implements MonkeyType {
        /** janitor monkey. */
        JANITOR
    }

    /**
     * The event types that this monkey causes.
     */
    public enum EventTypes implements EventType {
        /** Marking a resource as a cleanup candidate. */
        MARK_RESOURCE,
        /** Un-Marking a resource. */
        UNMARK_RESOURCE,
        /** Clean up a resource. */
        CLEANUP_RESOURCE,
        /** Opt in a resource. */
        OPT_IN_RESOURCE,
        /** Opt out a resource. */
        OPT_OUT_RESOURCE
    }

    /** {@inheritDoc} */
    @Override
    public final Type type() {
        return Type.JANITOR;
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
     * Opt in a resource for Janitor Monkey.
     * @param resourceId the resource id
     * @return the opt-in event
     */
    public abstract Event optInResource(String resourceId);

    /**
     * Opt out a resource for Janitor Monkey.
     * @param resourceId the resource id
     * @return the opt-out event
     */
    public abstract Event optOutResource(String resourceId);

    /**
     * Opt in a resource for Janitor Monkey.
     * @param resourceId the resource id
     * @param region the region of the resource
     * @return the opt-in event
     */
    public abstract Event optInResource(String resourceId, String region);

    /**
     * Opt out a resource for Janitor Monkey.
     * @param resourceId the resource id
     * @param region the region of the resource
     * @return the opt-out event
     */
    public abstract Event optOutResource(String resourceId, String region);

}

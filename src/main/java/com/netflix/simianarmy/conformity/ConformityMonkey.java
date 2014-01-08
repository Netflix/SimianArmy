/*
 *
 *  Copyright 2013 Netflix, Inc.
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
package com.netflix.simianarmy.conformity;

import com.netflix.simianarmy.Monkey;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyType;

import java.util.Collection;

/**
 * The abstract class for Conformity Monkey.
 */
public abstract class ConformityMonkey extends Monkey {

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
         * Crawler that gets information of all clusters for conformity check.
         * @return all clusters for conformity check
         */
        ClusterCrawler clusterCrawler();

        /**
         * Conformity rule engine.
         * @return the Conformity rule engine
         */
        ConformityRuleEngine ruleEngine();


        /**
         * Email notifier used to send notifications by the Conformity monkey.
         * @return the email notifier
         */
        ConformityEmailNotifier emailNotifier();

        /**
         * The regions the monkey is running in.
         * @return the regions the monkey is running in.
         */
        Collection<String> regions();

        /**
         * The tracker of the clusters for conformity monkey to check.
         * @return the tracker of the clusters for conformity monkey to check.
         */
        ConformityClusterTracker clusterTracker();

        /**
         * Gets the flag to indicate whether the monkey is leashed.
         * @return true if the monkey is leashed and does not make real change or send notifications to
         * cluster owners, false otherwise.
         */
        boolean isLeashed();
    }

    /** The context. */
    private final Context ctx;

    /**
     * Instantiates a new Conformity monkey.
     *
     * @param ctx
     *            the context.
     */
    public ConformityMonkey(Context ctx) {
        super(ctx);
        this.ctx = ctx;
    }

    /**
     * The monkey Type.
     */
    public enum Type implements MonkeyType {
        /** Conformity monkey. */
        CONFORMITY
    }

    /** {@inheritDoc} */
    @Override
    public final Type type() {
        return Type.CONFORMITY;
    }

    /** {@inheritDoc} */
    @Override
    public Context context() {
        return ctx;
    }

    /** {@inheritDoc} */
    @Override
    public abstract void doMonkeyBusiness();

}

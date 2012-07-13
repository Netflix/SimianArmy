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

public abstract class ChaosMonkey extends Monkey {

    public interface Context extends Monkey.Context {
        MonkeyConfiguration configuration();

        ChaosCrawler chaosCrawler();

        ChaosInstanceSelector chaosInstanceSelector();
    }

    private Context ctx;

    public ChaosMonkey(Context ctx) {
        super(ctx);
        this.ctx = ctx;
    }

    public enum Type {
        CHAOS
    }

    public enum EventTypes {
        CHAOS_TERMINATION
    }

    public final Enum type() {
        return Type.CHAOS;
    }

    public Context context() {
        return ctx;
    }

    public abstract void doMonkeyBusiness();

    public abstract boolean hasPreviousTerminations(ChaosCrawler.InstanceGroup group);

    public abstract void recordTermination(ChaosCrawler.InstanceGroup group, String instance);
}

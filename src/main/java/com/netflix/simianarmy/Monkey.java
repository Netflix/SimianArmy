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
package com.netflix.simianarmy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Monkey {
    private static final Logger LOGGER = LoggerFactory.getLogger(Monkey.class);

    public interface Context {
        MonkeyScheduler scheduler();

        MonkeyCalendar calendar();

        CloudClient cloudClient();

        MonkeyRecorder recorder();
    }

    private Context ctx;

    public Monkey(Context ctx) {
        this.ctx = ctx;
    }

    public abstract Enum type();

    public abstract void doMonkeyBusiness();

    public Context context() {
        return ctx;
    }

    public void run() throws Exception {
        if (ctx.calendar().isMonkeyTime(this)) {
            this.doMonkeyBusiness();
        } else {
            LOGGER.info("Not Time for " + this.type().name() + " Monkey");
        }
    }

    public void start() {
        final Monkey me = this;
        ctx.scheduler().start(type().name(), new Runnable() {
            public void run() {
                try {
                    me.run();
                } catch (Exception e) {
                    LOGGER.error(me.type().name() + " Monkey Error: ", e);
                }
            }
        });
    }

    public void stop() {
        ctx.scheduler().stop(type().name());
    }

}

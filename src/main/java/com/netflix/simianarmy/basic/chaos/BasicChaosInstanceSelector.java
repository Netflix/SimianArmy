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

import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.chaos.ChaosInstanceSelector;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

public class BasicChaosInstanceSelector implements ChaosInstanceSelector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChaosInstanceSelector.class);
    private static final Random RANDOM = new Random();

    protected Logger logger() {
        return LOGGER;
    }

    public String select(InstanceGroup group, double probability) {
        if (probability <= 0) {
            logger().info("Group {} [type {}] has disabled probability: {}",
                    new Object[] {group.name(), group.type(), probability});
            return null;
        }
        double rand = Math.random();
        if (rand > probability) {
            logger().info("Group {} [type {}] got lucky: {} > {}",
                    new Object[] {group.name(), group.type(), rand, probability});
            return null;
        }
        return group.instances().get(RANDOM.nextInt(group.instances().size()));
    }
}

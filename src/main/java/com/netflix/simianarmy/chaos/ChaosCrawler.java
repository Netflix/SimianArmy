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

import java.util.List;
import java.util.EnumSet;

/**
 * The Interface ChaosCrawler.
 */
public interface ChaosCrawler {

    /**
     * The Interface InstanceGroup.
     */
    public interface InstanceGroup {

        /**
         * Type.
         *
         * @return the group type enum
         */
        Enum type();

        /**
         * Name.
         *
         * @return the group string
         */
        String name();

        /**
         * Instances.
         *
         * @return the list of instances
         */
        List<String> instances();

        /**
         * Adds the instance.
         *
         * @param instance
         *            the instance
         */
        void addInstance(String instance);
    }

    /**
     * Group types.
     *
     * @return the type of groups this crawler creates \set
     */
    EnumSet<?> groupTypes();

    /**
     * Groups.
     *
     * @return the list
     */
    List<InstanceGroup> groups();
}

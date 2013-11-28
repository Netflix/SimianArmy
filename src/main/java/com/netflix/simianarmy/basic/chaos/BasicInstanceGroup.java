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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

/**
 * The Class BasicInstanceGroup.
 */
public class BasicInstanceGroup implements InstanceGroup {

    /** The name. */
    private final String name;

    /** The type. */
    private final GroupType type;

    /** The region. */
    private final String region;

    /**
     * Instantiates a new basic instance group.
     *
     * @param name
     *            the name
     * @param type
     *            the type
     */
    public BasicInstanceGroup(String name, GroupType type, String region) {
        this.name = name;
        this.type = type;
        this.region = region;
    }

    /** {@inheritDoc} */
    public GroupType type() {
        return type;
    }

    /** {@inheritDoc} */
    public String name() {
        return name;
    }

    /** {@inheritDoc} */
    public String region() {
        return region;
    }

    /** The list. */
    private List<String> list = new LinkedList<String>();

    /** {@inheritDoc} */
    @Override
    public List<String> instances() {
        return Collections.unmodifiableList(list);
    }

    /** {@inheritDoc} */
    @Override
    public void addInstance(String instance) {
        list.add(instance);
    }

    /** {@inheritDoc} */
    @Override
    public BasicInstanceGroup copyAs(String newName) {
        BasicInstanceGroup newGroup = new BasicInstanceGroup(newName, this.type(), this.region());
        for (String instance: this.instances()) {
            newGroup.addInstance(instance);
        }
        return newGroup;
    }
}

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
package com.netflix.simianarmy.basic;

import com.netflix.simianarmy.Instance;
import com.netflix.simianarmy.Tag;

import java.util.List;

public class BasicInstance implements Instance {

    private String instanceId;
    private String name;
    private String hostName;
    private List<Tag> tags;

    public BasicInstance(String instanceId) {
        this.instanceId = instanceId;
    }

    public BasicInstance(String instanceId, String name, String hostName, List<Tag> tags) {
        this.instanceId = instanceId;
        this.name       = name;
        this.hostName   = hostName;
        this.tags       = tags;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHostname() {
        return hostName;
    }

    @Override
    public List<Tag> getTags() {
        return tags;
    }
}

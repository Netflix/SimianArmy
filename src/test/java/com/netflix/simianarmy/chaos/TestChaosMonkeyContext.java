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

import com.netflix.simianarmy.TestMonkeyContext;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

import java.util.Properties;
import java.io.InputStream;

import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.EnumSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestChaosMonkeyContext extends TestMonkeyContext implements ChaosMonkey.Context {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestChaosMonkeyContext.class);
    private BasicConfiguration cfg;

    public TestChaosMonkeyContext() {
        super(ChaosMonkey.Type.CHAOS);
        cfg = new BasicConfiguration(new Properties());
    }

    public TestChaosMonkeyContext(String propFile) {
        super(ChaosMonkey.Type.CHAOS);
        Properties props = new Properties();
        try {
            InputStream is = TestChaosMonkeyContext.class.getResourceAsStream(propFile);
            try {
                props.load(is);
            } finally {
                is.close();
            }
        } catch (Exception e) {
            LOGGER.error("Unable to load properties file " + propFile, e);
        }
        cfg = new BasicConfiguration(props);
    }

    public MonkeyConfiguration configuration() {
        return cfg;
    }

    public static class TestInstanceGroup implements InstanceGroup {
        private final Enum type;
        private final String name;
        private final String instance;

        public TestInstanceGroup(Enum type, String name, String instance) {
            this.type = type;
            this.name = name;
            this.instance = instance;
        }

        public Enum type() {
            return type;
        }

        public String name() {
            return name;
        }

        public List<String> instances() {
            return Arrays.asList(instance);
        }

        public void addInstance(String ignored) {
        }
    }

    public enum CrawlerTypes {
        TYPE_A, TYPE_B
    };

    public ChaosCrawler chaosCrawler() {
        return new ChaosCrawler() {
            public EnumSet<?> groupTypes() {
                return EnumSet.allOf(CrawlerTypes.class);
            }

            public List<InstanceGroup> groups() {
                InstanceGroup gA0 = new TestInstanceGroup(CrawlerTypes.TYPE_A, "name0", "0:i-123456780");
                InstanceGroup gA1 = new TestInstanceGroup(CrawlerTypes.TYPE_A, "name1", "1:i-123456781");
                InstanceGroup gB2 = new TestInstanceGroup(CrawlerTypes.TYPE_B, "name2", "2:i-123456782");
                InstanceGroup gB3 = new TestInstanceGroup(CrawlerTypes.TYPE_B, "name3", "3:i-123456783");
                return Arrays.asList(gA0, gA1, gB2, gB3);
            }
        };
    }

    private List<InstanceGroup> selectedOn = new LinkedList<InstanceGroup>();

    public List<InstanceGroup> selectedOn() {
        return selectedOn;
    }

    public ChaosInstanceSelector chaosInstanceSelector() {
        return new ChaosInstanceSelector() {
            public String select(InstanceGroup group, double probability) {
                selectedOn.add(group);
                return super.select(group, probability);
            }
        };
    }

    private List<String> terminated = new LinkedList<String>();

    public List<String> terminated() {
        return terminated;
    }

    public CloudClient cloudClient() {
        return new CloudClient() {
            public void terminateInstance(String instanceId) {
                terminated.add(instanceId);
            }
        };
    }

}

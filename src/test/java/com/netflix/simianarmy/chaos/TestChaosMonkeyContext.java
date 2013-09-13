// CHECKSTYLE IGNORE Javadoc
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.TestMonkeyContext;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.chaos.BasicChaosInstanceSelector;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

public class TestChaosMonkeyContext extends TestMonkeyContext implements ChaosMonkey.Context {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestChaosMonkeyContext.class);
    private final BasicConfiguration cfg;

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

    @Override
    public MonkeyConfiguration configuration() {
        return cfg;
    }

    public static class TestInstanceGroup implements InstanceGroup {
        private final Enum type;
        private final String name;
        private final String region;
        private final List<String> instances = new ArrayList<String>();

        public TestInstanceGroup(Enum type, String name, String region, String... instances) {
            this.type = type;
            this.name = name;
            this.region = region;
            for (String i : instances) {
                this.instances.add(i);
            }
        }

        @Override
        public Enum type() {
            return type;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String region() {
            return region;
        }

        @Override
        public List<String> instances() {
            return Collections.unmodifiableList(instances);
        }

        @Override
        public void addInstance(String ignored) {
        }

        public void deleteInstance(String id) {
            instances.remove(id);
        }

        @Override
        public InstanceGroup copyAs(String newName) {
            return new TestInstanceGroup(this.type, newName, this.region, instances().toString());
        }
    }

    public enum CrawlerTypes {
        TYPE_A, TYPE_B, TYPE_C, TYPE_D
    };

    @Override
    public ChaosCrawler chaosCrawler() {
        return new ChaosCrawler() {
            @Override
            public EnumSet<?> groupTypes() {
                return EnumSet.allOf(CrawlerTypes.class);
            }

            @Override
            public List<InstanceGroup> groups() {
                InstanceGroup gA0 = new TestInstanceGroup(CrawlerTypes.TYPE_A, "name0", "reg1", "0:i-123456780");
                InstanceGroup gA1 = new TestInstanceGroup(CrawlerTypes.TYPE_A, "name1", "reg1", "1:i-123456781");
                InstanceGroup gB2 = new TestInstanceGroup(CrawlerTypes.TYPE_B, "name2", "reg1", "2:i-123456782");
                InstanceGroup gB3 = new TestInstanceGroup(CrawlerTypes.TYPE_B, "name3", "reg1", "3:i-123456783");
                InstanceGroup gC1 = new TestInstanceGroup(CrawlerTypes.TYPE_C, "name4", "reg1", "3:i-123456784",
                        "3:i-123456785");
                InstanceGroup gC2 = new TestInstanceGroup(CrawlerTypes.TYPE_C, "name5", "reg1", "3:i-123456786",
                        "3:i-123456787");
                InstanceGroup gD0 = new TestInstanceGroup(CrawlerTypes.TYPE_D, "new-group-TestGroup1-XXXXXXXXX",
                        "reg1", "3:i-123456786", "3:i-123456787");
                return Arrays.asList(gA0, gA1, gB2, gB3, gC1, gC2, gD0);
            }

            @Override
            public List<InstanceGroup> groups(String... names) {
                Map<String, InstanceGroup> nameToGroup = new HashMap<String, InstanceGroup>();
                for (InstanceGroup ig : groups()) {
                    nameToGroup.put(ig.name(), ig);
                }
                List<InstanceGroup> list = new LinkedList<InstanceGroup>();
                for (String name : names) {
                    InstanceGroup ig = nameToGroup.get(name);
                    if (ig == null) {
                        continue;
                    }
                    for (String instanceId : terminated) {
                        // Remove terminated instances from crawler list
                        TestInstanceGroup testIg = (TestInstanceGroup) ig;
                        testIg.deleteInstance(instanceId);
                    }
                    list.add(ig);
                }
                return list;
            }
        };
    }
    private final List<InstanceGroup> selectedOn = new LinkedList<InstanceGroup>();

    public List<InstanceGroup> selectedOn() {
        return selectedOn;
    }

    @Override
    public ChaosInstanceSelector chaosInstanceSelector() {
        return new BasicChaosInstanceSelector() {
            @Override
            public Collection<String> select(InstanceGroup group, double probability) {
                selectedOn.add(group);
                return super.select(group, probability);
            }
        };
    }

    private final List<String> terminated = new LinkedList<String>();

    public List<String> terminated() {
        return terminated;
    }

    @Override
    public CloudClient cloudClient() {
        return new CloudClient() {
            @Override
            public void terminateInstance(String instanceId) {
                terminated.add(instanceId);
            }

            @Override
            public void setSecurityGroups(String instanceId, List<String> groups) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void createTagsForResources(Map<String, String> keyValueMap, String... resourceIds) {
            }

            @Override
            public void deleteAutoScalingGroup(String asgName) {
            }

            @Override
            public void deleteVolume(String volumeId) {
            }

            @Override
            public void deleteSnapshot(String snapshotId) {
            }

            @Override
            public void deleteImage(String imageId) {
            }

            @Override
            public void deleteLaunchConfiguration(String launchConfigName) {
            }
        };
    }

    private int groupNotified = 0;
    private int globallyNotified = 0;

    @Override
    public ChaosEmailNotifier chaosEmailNotifier() {
        return new ChaosEmailNotifier(null) {
            @Override
            public String getSourceAddress(String to) {
                return "source@chaosMonkey.foo";
            }

            @Override
            public String[] getCcAddresses(String to) {
                return new String[] {};
            }

            @Override
            public String buildEmailSubject(String to) {
                return String.format("Testing Chaos termination notification for %s", to);
            }

            @Override
            public void sendTerminationNotification(InstanceGroup group, String instance) {
                groupNotified++;
            }

            @Override
            public void sendTerminationGlobalNotification(InstanceGroup group, String instance) {
                globallyNotified++;
            }
        };
    }

    public int getNotified() {
        return groupNotified;
    }

    public int getGloballyNotified() {
        return globallyNotified;
    }

}

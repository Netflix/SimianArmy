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
// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.basic.chaos;

import java.util.*;

import com.amazonaws.services.autoscaling.model.TagDescription;
import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.chaos.ChaosInstanceSelector;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.testng.Assert;
import org.slf4j.Logger;

import static org.slf4j.helpers.NOPLogger.NOP_LOGGER;

// CHECKSTYLE IGNORE MagicNumberCheck
public class TestBasicChaosInstanceSelector {
    private ChaosInstanceSelector selector = new BasicChaosInstanceSelector() {
        // turn off selector logger for this test since we call it ~1M times
        protected Logger logger() {
            return NOP_LOGGER;
        }
    };

    public enum Types implements GroupType {
        TEST
    }

    private InstanceGroup group = new InstanceGroup() {
        public GroupType type() {
            return Types.TEST;
        }

        public String name() {
            return "TestGroup";
        }

        public String region() {
            return "region";
        }

        public List<TagDescription> tags() {
            return Collections.<TagDescription>emptyList();
        }

        public List<String> instances() {
            return Arrays.asList("i-123456789012345670", "i-123456789012345671", "i-123456789012345672", "i-123456789012345673", "i-123456789012345674",
                    "i-123456789012345675", "i-123456789012345676", "i-123456789012345677", "i-123456789012345678", "i-123456789012345679");
        }

        public void addInstance(String ignored) {
        }

        @Override
        public InstanceGroup copyAs(String name) {
            return this;
        }
    };

    @Test
    public void testSelect() {
        Assert.assertTrue(selector.select(group, 0).isEmpty(), "select disabled group is always null");
        Assert.assertTrue(selector.select(group, 0.0).isEmpty(), "select disabled group is always null");

        int selected = 0;
        for (int i = 0; i < 100; i++) {
            selected += selector.select(group, 1.0).size();
        }

        Assert.assertEquals(selected, 100, "1.0 probability always selects an instance");

    }

    @DataProvider
    public Object[][] evenSelectionDataProvider() {
        return new Object[][] {{1.0}, {0.9}, {0.8}, {0.7}, {0.6}, {0.5}, {0.4}, {0.3}, {0.2},
                {0.1} };
    }

    static final int RUNS = 1000000;

    @Test(dataProvider = "evenSelectionDataProvider")
    public void testEvenSelections(double probability) {

        Map<String, Integer> selectMap = new HashMap<String, Integer>();
        for (int i = 0; i < RUNS; i++) {
            Collection<String> instances = selector.select(group, probability);
            for (String inst : instances) {
                if (selectMap.containsKey(inst)) {
                    selectMap.put(inst, selectMap.get(inst) + 1);
                } else {
                    selectMap.put(inst, 1);
                }
            }
        }

        Assert.assertEquals(selectMap.size(), group.instances().size(), "verify we selected all instances");

        // allow for 4% variation over all the selection runs
        int avg = Double.valueOf((RUNS / (double) group.instances().size()) * probability).intValue();
        int max = Double.valueOf(avg + (avg * 0.04)).intValue();
        int min = Double.valueOf(avg - (avg * 0.04)).intValue();

        for (Map.Entry<String, Integer> pair : selectMap.entrySet()) {
            Assert.assertTrue(pair.getValue() > min && pair.getValue() < max, pair.getKey() + " selected " + avg
                    + " +- 4% times for prob: " + probability + " [got: " + pair.getValue() + "]");
        }
    }

    @Test
    public void testSelectWithProbMoreThanOne() {
        // The number of selected instances should always be p when the prob is an integer.
        for (int p = 0; p <= group.instances().size(); p++) {
            Assert.assertEquals(selector.select(group, p).size(), p);
        }

        // When the prob is bigger than the size of the group, we get the whole group.
        for (int p = group.instances().size(); p <= group.instances().size() * 2; p++) {
            Assert.assertEquals(selector.select(group, p).size(), group.instances().size());
        }
    }

    @Test
    public void testSelectWithProbMoreThanOneWithFraction() {
        // The number of selected instances can be p or p+1, depending on whether the fraction part
        // can get a instance selected.
        for (int p = 0; p <= group.instances().size(); p++) {
            Collection<String> selected = selector.select(group, p + 0.5);
            Assert.assertTrue(selected.size() >= p && selected.size() <= p + 1);
        }

        // When the prob is bigger than the size of the group, we get the whole group.
        for (int p = group.instances().size(); p <= group.instances().size() * 2; p++) {
            Collection<String> selected = selector.select(group, p + 0.5);
            Assert.assertEquals(selected.size(), group.instances().size());
        }
    }
}

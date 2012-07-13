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

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
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

    public enum Types {
        TEST
    }

    private InstanceGroup group = new InstanceGroup() {
        public Enum type() {
            return Types.TEST;
        }

        public String name() {
            return "TestGroup";
        }

        public List<String> instances() {
            return Arrays.asList("i-123456780", "i-123456781", "i-123456782", "i-123456783", "i-123456784",
                    "i-123456785", "i-123456786", "i-123456787", "i-123456788", "i-123456789");
        }

        public void addInstance(String ignored) {
        }
    };

    @Test
    public void testSelect() {
        Assert.assertNull(selector.select(group, 0), "select disabled group is always null");
        Assert.assertNull(selector.select(group, 0.0), "select disabled group is always null");

        int selected = 0;
        for (int i = 0; i < 100; i++) {
            if (selector.select(group, 1.0) != null) {
                selected++;
            }
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
            String inst = selector.select(group, probability);
            if (inst == null) {
                continue;
            }
            if (selectMap.containsKey(inst)) {
                selectMap.put(inst, selectMap.get(inst) + 1);
            } else {
                selectMap.put(inst, 1);
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
}

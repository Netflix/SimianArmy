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
package com.netflix.simianarmy.basic;

import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Map;
import java.util.HashMap;

public class TestBasicRecorderEvent {
    public enum Types {
        MONKEY, EVENT
    };

    @Test
    public void test() {
        Enum monkeyType = Types.MONKEY;
        Enum eventType = Types.EVENT;
        BasicRecorderEvent evt = new BasicRecorderEvent(monkeyType, eventType, "test-id");
        testEvent(evt);

        // CHECKSTYLE IGNORE MagicNumberCheck
        long time = 1330538400000L;
        evt = new BasicRecorderEvent(monkeyType, eventType, "test-id", time);
        testEvent(evt);
        Assert.assertEquals(evt.eventTime().getTime(), time);

    }

    void testEvent(BasicRecorderEvent evt) {
        Assert.assertEquals(evt.id(), "test-id");
        Assert.assertEquals(evt.monkeyType(), Types.MONKEY);
        Assert.assertEquals(evt.eventType(), Types.EVENT);
        Assert.assertEquals(evt.addField("a", "1"), evt);
        Map<String, String> map = new HashMap<String, String>();
        map.put("b", "2");
        map.put("c", "3");

        Assert.assertEquals(evt.addFields(map), evt);
        Assert.assertEquals(evt.field("a"), "1");
        Assert.assertEquals(evt.field("b"), "2");
        Assert.assertEquals(evt.field("c"), "3");
        Map<String, String> f = evt.fields();
        Assert.assertEquals(f.get("a"), "1");
        Assert.assertEquals(f.get("b"), "2");
        Assert.assertEquals(f.get("c"), "3");
    }
}

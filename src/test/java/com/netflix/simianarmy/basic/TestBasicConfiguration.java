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
package com.netflix.simianarmy.basic;

import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Properties;

public class TestBasicConfiguration extends BasicConfiguration {
    private static final Properties PROPS = new Properties();

    public TestBasicConfiguration() {
        super(PROPS);
    }

    @Test
    public void testGetBool() {
        PROPS.clear();
        Assert.assertFalse(getBool("foobar.enabled"));

        PROPS.setProperty("foobar.enabled", "true");
        Assert.assertTrue(getBool("foobar.enabled"));

        PROPS.setProperty("foobar.enabled", "false");
        Assert.assertFalse(getBool("foobar.enabled"));
    }

    @Test
    public void testGetBoolOrElse() {
        PROPS.clear();
        Assert.assertFalse(getBoolOrElse("foobar.enabled", false));
        Assert.assertTrue(getBoolOrElse("foobar.enabled", true));

        PROPS.setProperty("foobar.enabled", "true");
        Assert.assertTrue(getBoolOrElse("foobar.enabled", false));
        Assert.assertTrue(getBoolOrElse("foobar.enabled", true));

        PROPS.setProperty("foobar.enabled", "false");
        Assert.assertFalse(getBoolOrElse("foobar.enabled", false));
        Assert.assertFalse(getBoolOrElse("foobar.enabled", true));
    }

    @Test
    public void testGetNumOrElse() {
        // CHECKSTYLE IGNORE MagicNumberCheck
        PROPS.clear();
        Assert.assertEquals(getNumOrElse("foobar.number", 42), 42D);

        PROPS.setProperty("foobar.number", "0");
        Assert.assertEquals(getNumOrElse("foobar.number", 42), 0D);
    }

    @Test
    public void testGetStr() {
        PROPS.clear();
        Assert.assertNull(getStr("foobar"));

        PROPS.setProperty("foobar", "string");
        Assert.assertEquals(getStr("foobar"), "string");
    }

    @Test
    public void testGetStrOrElse() {
        PROPS.clear();
        Assert.assertEquals(getStrOrElse("foobar", "default"), "default");

        PROPS.setProperty("foobar", "string");
        Assert.assertEquals(getStrOrElse("foobar", "default"), "string");

        PROPS.setProperty("foobar", "");
        Assert.assertEquals(getStrOrElse("foobar", "default"), "");
    }
}

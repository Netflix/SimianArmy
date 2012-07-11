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

import java.util.Properties;

import com.netflix.simianarmy.MonkeyConfiguration;

public class BasicConfiguration implements MonkeyConfiguration {

    private Properties props;

    public BasicConfiguration(Properties props) {
        this.props = props;
    }

    @Override
    public boolean getBool(String property) {
        return getBoolOrElse(property, false);
    }

    @Override
    public boolean getBoolOrElse(String property, boolean dflt) {
        String val = props.getProperty(property);
        return val == null ? dflt : Boolean.parseBoolean(val);
    }

    @Override
    public double getNumOrElse(String property, double dflt) {
        String val = props.getProperty(property);
        return val == null ? dflt : Double.parseDouble(val);
    }

    @Override
    public String getStr(String property) {
        return getStrOrElse(property, null);
    }

    @Override
    public String getStrOrElse(String property, String dflt) {
        String val = props.getProperty(property);
        return val == null ? dflt : val;
    }

    @Override
    public void reload() {
        // BasicConfiguration is based on static properties, so reload is a no-op
    }
}

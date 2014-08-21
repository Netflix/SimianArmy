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

import com.netflix.simianarmy.MonkeyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Properties;

/**
 * The Class BasicConfiguration.
 */
public class BasicConfiguration implements MonkeyConfiguration {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicConfiguration.class);

    /** The properties. */
    private Properties props;

    /**
     * Instantiates a new basic configuration.
     * @param props
     *            the properties
     */
    public BasicConfiguration(Properties props) {
        this.props = props;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getBool(String property) {
        return getBoolOrElse(property, false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getBoolOrElse(String property, boolean dflt) {
        String val = props.getProperty(property);
        if (val == null) {
            return dflt;
        }
        val = val.trim();
        return Boolean.parseBoolean(val);
    }

    /** {@inheritDoc} */
    @Override
    public double getNumOrElse(String property, double dflt) {
        String val = props.getProperty(property);
        double result = dflt;
        if (val != null && !val.isEmpty()) {
            try {
                result = Double.parseDouble(val);
            } catch (NumberFormatException e) {
                LOGGER.error("failed to parse property: " + property + "; returning default value: " + dflt, e);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String getStr(String property) {
        return getStrOrElse(property, null);
    }

    /** {@inheritDoc} */
    @Override
    public String getStrOrElse(String property, String dflt) {
        String val = props.getProperty(property);
        return val == null ? dflt : val;
    }

    /** {@inheritDoc} */
    @Override
    public void reload() {
        // BasicConfiguration is based on static properties, so reload is a no-op
    }

    @Override
    public void reload(String groupName) {
        // BasicConfiguration is based on static properties, so reload is a no-op
    }
}

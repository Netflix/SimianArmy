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

package com.netflix.simianarmy.aws.janitor.rule.generic;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.janitor.Rule;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * A rule for excluding resources that contain the provided tags (name and value).
 *
 * If a resource contains the tag and the appropriate value, it will be excluded from any
 * other janitor rules and will not be cleaned.
 *
 */
public class TagValueExclusionRule implements Rule {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TagValueExclusionRule.class);
    private final Map<String,String> tags;

    /**
     * Constructor for TagValueExclusionRule.
     *
     * @param tags
     *            Set of tags and values to match for exclusion
     */
    public TagValueExclusionRule(Map<String, String> tags) {
        this.tags = tags;
    }

    /**
     * Constructor for TagValueExclusionRule.  Use this constructor to pass names and values as separate args.
     * This is intended for convenience when specifying tag names/values in property files.
     *
     * Each tag[i] = (name[i], value[i])
     *
     * @param names
     *            Set of names to match for exclusion.  Size of names must match size of values.
     * @param values
     *            Set of values to match for exclusion.  Size of names must match size of values.
     */
    public TagValueExclusionRule(String[] names, String[] values) {
        tags = new HashMap<String,String>();
        int i = 0;
        for(String name : names) {
            tags.put(name, values[i]);
            i++;
        }
    }

    @Override
    public boolean isValid(Resource resource) {
        Validate.notNull(resource);
        for (String tagName : tags.keySet()) {
            String resourceValue = resource.getTag(tagName);
            if (resourceValue != null && resourceValue.equals(tags.get(tagName))) {
                LOGGER.debug(String.format("The resource %s has the exclusion tag %s with value %s", resource.getId(), tagName, resourceValue));
                return true;
            }
        }
        return false;
    }
}

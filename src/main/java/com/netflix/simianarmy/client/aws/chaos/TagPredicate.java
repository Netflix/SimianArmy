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
package com.netflix.simianarmy.client.aws.chaos;

import com.amazonaws.services.autoscaling.model.TagDescription;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.netflix.simianarmy.chaos.ChaosCrawler;

/**
 *  * The Class TagPredicate. This will apply the tag-key and the tag-value filter on the list of InstanceGroups .
 */
public class TagPredicate implements Predicate<ChaosCrawler.InstanceGroup> {

    private final String key, value;

    public TagPredicate(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean apply(ChaosCrawler.InstanceGroup instanceGroup) {
        return Iterables.any(instanceGroup.tags(), new com.google.common.base.Predicate<TagDescription>() {
            @Override
            public boolean apply(TagDescription tagDescription) {
                return tagDescription.getKey().equals(key) && tagDescription.getValue().equals(value);
            }
        });
    }
}

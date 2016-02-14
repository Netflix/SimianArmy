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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.netflix.simianarmy.chaos.ChaosCrawler;

import java.util.EnumSet;
import java.util.List;

/**
 * The Class FilteringChaosCrawler. This will filter the result from ASGChaosCrawler for all available AutoScalingGroups associated with the AWS account based on requested filter.
 */
public class FilteringChaosCrawler implements ChaosCrawler {

    private final ChaosCrawler crawler;
    private final Predicate<? super InstanceGroup> predicate;

    public FilteringChaosCrawler(ChaosCrawler crawler, Predicate<? super InstanceGroup> predicate) {
        this.crawler = crawler;
        this.predicate = predicate;
    }

    /** {@inheritDoc} */
    @Override
    public EnumSet<?> groupTypes() {
        return crawler.groupTypes();
    }

    /** {@inheritDoc} */
    @Override
    public List<InstanceGroup> groups() {
        return filter(crawler.groups());
    }

    /** {@inheritDoc} */
    @Override
    public List<InstanceGroup> groups(String... names) {
        return filter(crawler.groups(names));
    }


    /**
     * Return the filtered list of InstanceGroups using the requested predicate. The filter is applied on the InstanceGroup retrieved from the ASGChaosCrawler class.
     * @param list list of InstanceGroups result of the chaos crawler
     * @return The appropriate {@link InstanceGroup}
     */
    protected List<InstanceGroup> filter(List<InstanceGroup> list) {
        return Lists.newArrayList(Iterables.filter(list, predicate));
    }
}

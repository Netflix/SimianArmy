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

import java.util.*;

import com.google.common.collect.Lists;
import com.netflix.simianarmy.Instance;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosInstanceSelector;
import com.netflix.simianarmy.Tag;
import com.netflix.simianarmy.NoInstanceWithTagsFoundException;
/**
 * The Class BasicChaosInstanceSelector.
 */
public class BasicChaosInstanceSelector implements ChaosInstanceSelector {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicChaosInstanceSelector.class);

    /** The Constant RANDOM. */
    private static final Random RANDOM = new Random();

    /**
     * Logger, this is abstracted so subclasses (for testing) can reset logger to make it less verbose.
     * @return the logger
     */
    protected Logger logger() {
        return LOGGER;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> select(InstanceGroup group, double probability) {
        int n = ((int) probability);
        String selected = selectOneInstance(group, probability - n);
        Collection<String> result = selectNInstances(group.instanceIds(), n, selected);
        if (selected != null) {
            result.add(selected);
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> selectOneByTags(InstanceGroup group, List<Tag> ec2TagsSent)  throws NoInstanceWithTagsFoundException {
        List<String> matchInstances = new ArrayList<String>();
        Set<Tag> ec2TagsSentSet = new HashSet<Tag>(ec2TagsSent);

        for (Instance inst : group.instances()) {
            Set<Tag> ec2TagsInstSet = new HashSet<Tag>(inst.getTags());
            if (ec2TagsInstSet.containsAll(ec2TagsSentSet))
                matchInstances.add(inst.getInstanceId());
        }
        Collection<String> result = null;
        if (matchInstances.size() > 0){
             result = selectNInstances(matchInstances, 1, null);
        }
        if (result == null) {
            logger().info("No instances with those sent Tags were found");
            throw new NoInstanceWithTagsFoundException(ec2TagsSent);
        }
        return result;
    }

    private Collection<String> selectNInstances(Collection<String> instances, int n, String selected) {
        logger().info("Randomly selecting {} from {} instances, excluding {}",
                new Object[] {n, instances.size(), selected});
        List<String> copy = Lists.newArrayList();
        for (String instance : instances) {
            if (!instance.equals(selected)) {
                copy.add(instance);
            }
        }
        if (n >= copy.size()) {
            return copy;
        }
        Collections.shuffle(copy);
        return copy.subList(0, n);
    }

    private String selectOneInstance(InstanceGroup group, double probability) {
        Validate.isTrue(probability < 1);
        if (probability <= 0) {
            logger().info("Group {} [type {}] has disabled probability: {}",
                    new Object[] {group.name(), group.type(), probability});
            return null;
        }
        double rand = Math.random();
        if (rand > probability || group.instances().isEmpty()) {
            logger().info("Group {} [type {}] got lucky: {} > {}",
                    new Object[] {group.name(), group.type(), rand, probability});
            return null;
        }
        return group.instanceIds().get(RANDOM.nextInt(group.instanceIds().size()));
    }
}

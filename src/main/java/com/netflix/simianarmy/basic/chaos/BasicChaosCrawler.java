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
import java.util.LinkedList;
import java.util.EnumSet;

import com.netflix.simianarmy.aws.AWSClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;

import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

/**
 * The Class BasicChaosCrawler. This will crawl for all available AutoScalingGroups associated with the AWS account.
 */
public class BasicChaosCrawler implements ChaosCrawler {

    /**
     * The group types Types.
     */
    public enum Types {

        /** only crawls AutoScalingGroups. */
        ASG;
    }

    /** The aws client. */
    private AWSClient awsClient;

    /**
     * Instantiates a new basic chaos crawler.
     *
     * @param awsClient
     *            the aws client
     */
    public BasicChaosCrawler(AWSClient awsClient) {
        this.awsClient = awsClient;
    }

    /**
     * The Class BasicInstanceGroup.
     */
    public static class BasicInstanceGroup implements InstanceGroup {

        /** The name. */
        private final String name;

        /** The type. */
        private final Enum type;

        /**
         * Instantiates a new basic instance group.
         *
         * @param name
         *            the name
         */
        public BasicInstanceGroup(String name) {
            this.name = name;
            this.type = Types.ASG;
        }

        /**
         * Instantiates a new basic instance group.
         *
         * @param name
         *            the name
         * @param type
         *            the type
         */
        public BasicInstanceGroup(String name, Enum type) {
            this.name = name;
            this.type = type;
        }

        /** {@inheritDoc} */
        public Enum type() {
            return type;
        }

        /** {@inheritDoc} */
        public String name() {
            return name;
        }

        /** The list. */
        private List<String> list = new LinkedList<String>();

        /** {@inheritDoc} */
        @Override
        public List<String> instances() {
            return list;
        }

        /** {@inheritDoc} */
        @Override
        public void addInstance(String instance) {
            list.add(instance);
        }
    }

    /** {@inheritDoc} */
    @Override
    public EnumSet<?> groupTypes() {
        return EnumSet.allOf(Types.class);
    }

    /** {@inheritDoc} */
    @Override
    public List<InstanceGroup> groups() {
        List<InstanceGroup> list = new LinkedList<InstanceGroup>();
        for (AutoScalingGroup asg : awsClient.describeAutoScalingGroups()) {
            InstanceGroup ig = new BasicInstanceGroup(asg.getAutoScalingGroupName());
            for (Instance inst : asg.getInstances()) {
                ig.addInstance(inst.getInstanceId());
            }
            list.add(ig);
        }
        return list;
    }
}

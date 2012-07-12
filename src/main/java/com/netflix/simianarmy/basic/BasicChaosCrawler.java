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

import java.util.List;
import java.util.LinkedList;
import java.util.EnumSet;

import com.netflix.simianarmy.aws.AWSClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;

import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

public class BasicChaosCrawler implements ChaosCrawler {

    public enum Types {
        ASG;
    }

    private AWSClient awsClient;

    public BasicChaosCrawler(AWSClient awsClient) {
        this.awsClient = awsClient;
    }

    public static class BasicInstanceGroup implements InstanceGroup {
        private final String name;
        private final Enum type;

        BasicInstanceGroup(String name) {
            this.name = name;
            this.type = Types.ASG;
        }

        BasicInstanceGroup(String name, Enum type) {
            this.name = name;
            this.type = type;
        }

        public Enum type() {
            return type;
        }

        public String name() {
            return name;
        }

        private List<String> list = new LinkedList<String>();

        @Override
        public List<String> instances() {
            return list;
        }

        @Override
        public void addInstance(String instance) {
            list.add(instance);
        }
    }

    @Override
    public EnumSet<?> groupTypes() {
        return EnumSet.allOf(Types.class);
    }

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

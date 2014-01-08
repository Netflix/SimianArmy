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

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.client.aws.AWSClient;

/**
 * The Class ASGChaosCrawler. This will crawl for all available AutoScalingGroups associated with the AWS account.
 */
public class ASGChaosCrawler implements ChaosCrawler {

    /**
     * The group types Types.
     */
    public enum Types implements GroupType {

        /** only crawls AutoScalingGroups. */
        ASG;
    }

    /** The aws client. */
    private final AWSClient awsClient;

    /**
     * Instantiates a new basic chaos crawler.
     *
     * @param awsClient
     *            the aws client
     */
    public ASGChaosCrawler(AWSClient awsClient) {
        this.awsClient = awsClient;
    }

    /** {@inheritDoc} */
    @Override
    public EnumSet<?> groupTypes() {
        return EnumSet.allOf(Types.class);
    }

    /** {@inheritDoc} */
    @Override
    public List<InstanceGroup> groups() {
        return groups((String[]) null);
    }

    @Override
    public List<InstanceGroup> groups(String... names) {
        List<InstanceGroup> list = new LinkedList<InstanceGroup>();
        for (AutoScalingGroup asg : awsClient.describeAutoScalingGroups(names)) {
            InstanceGroup ig = new BasicInstanceGroup(asg.getAutoScalingGroupName(), Types.ASG, awsClient.region());
            for (Instance inst : asg.getInstances()) {
                ig.addInstance(inst.getInstanceId());
            }
            list.add(ig);
        }
        return list;
    }
}

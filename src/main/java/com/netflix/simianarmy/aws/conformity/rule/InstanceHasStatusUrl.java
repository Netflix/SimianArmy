/*
 *
 *  Copyright 2013 Netflix, Inc.
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
package com.netflix.simianarmy.aws.conformity.rule;

import com.google.common.collect.Lists;
import com.netflix.simianarmy.conformity.AutoScalingGroup;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.Conformity;
import com.netflix.simianarmy.conformity.ConformityRule;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * The class implementing a conformity rule that checks if all instances in a cluster has status url.
 */
public class InstanceHasStatusUrl implements ConformityRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceHasStatusUrl.class);

    private static final String RULE_NAME = "InstanceHasStatusUrl";
    private static final String REASON = "Status url not defined";

    private final ConformityEurekaClient conformityEurekaClient;

    /**
     * Constructor.
     * @param conformityEurekaClient
     *          the client to access the Discovery/Eureka service for checking the status of instances.
     */
    public InstanceHasStatusUrl(ConformityEurekaClient conformityEurekaClient) {
        Validate.notNull(conformityEurekaClient);
        this.conformityEurekaClient = conformityEurekaClient;
    }

    @Override
    public Conformity check(Cluster cluster) {
        Collection<String> failedComponents = Lists.newArrayList();
        for (AutoScalingGroup asg : cluster.getAutoScalingGroups()) {
            if (asg.isSuspended()) {
                continue;
            }
            for (String instance : asg.getInstances()) {
                if (!conformityEurekaClient.hasStatusUrl(cluster.getRegion(), instance)) {
                    LOGGER.info(String.format("Instance %s does not have a status page url in discovery.",
                            instance));
                    failedComponents.add(instance);
                }
            }
        }
        return new Conformity(getName(), failedComponents);
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

    @Override
    public String getNonconformingReason() {
        return REASON;
    }
}

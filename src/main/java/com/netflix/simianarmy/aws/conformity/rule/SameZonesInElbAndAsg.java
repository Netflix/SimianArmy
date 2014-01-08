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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.conformity.AutoScalingGroup;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.Conformity;
import com.netflix.simianarmy.conformity.ConformityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The class implementing a conformity rule that checks if the zones in ELB and ASG are the same.
 */
public class SameZonesInElbAndAsg implements ConformityRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceHasStatusUrl.class);

    private final Map<String, AWSClient> regionToAwsClient = Maps.newHashMap();

    private AWSCredentialsProvider awsCredentialsProvider;

    private static final String RULE_NAME = "SameZonesInElbAndAsg";
    private static final String REASON = "Availability zones of ELB and ASG are different";

    /**
     * Constructs an instance with the default AWS credentials provider chain.
     * @see com.amazonaws.auth.DefaultAWSCredentialsProviderChain
     */
    public SameZonesInElbAndAsg() {
        this(new DefaultAWSCredentialsProviderChain());
    }

    /**
     * Constructs an instance with the passed AWS Credential Provider.
     * @param awsCredentialsProvider
     */
    public SameZonesInElbAndAsg(AWSCredentialsProvider awsCredentialsProvider) {
        this.awsCredentialsProvider = awsCredentialsProvider;
    }

    @Override
    public Conformity check(Cluster cluster) {
        List<String> asgNames = Lists.newArrayList();
        for (AutoScalingGroup asg : cluster.getAutoScalingGroups()) {
            asgNames.add(asg.getName());
        }
        Collection<String> failedComponents = Lists.newArrayList();
        for (AutoScalingGroup asg : cluster.getAutoScalingGroups()) {
            List<String> asgZones = getAvailabilityZonesForAsg(cluster.getRegion(), asg.getName());
            for (String lbName : getLoadBalancerNamesForAsg(cluster.getRegion(), asg.getName())) {
                List<String> lbZones = getAvailabilityZonesForLoadBalancer(cluster.getRegion(), lbName);
                if (!haveSameZones(asgZones, lbZones)) {
                    LOGGER.info(String.format("ASG %s and ELB %s do not have the same availability zones",
                            asgZones, lbZones));
                    failedComponents.add(lbName);
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

    /**
     * Gets the load balancer names of an ASG. Can be overridden in subclasses.
     * @param region the region
     * @param asgName the ASG name
     * @return the list of load balancer names
     */
    protected List<String> getLoadBalancerNamesForAsg(String region, String asgName) {
        List<com.amazonaws.services.autoscaling.model.AutoScalingGroup> asgs =
                getAwsClient(region).describeAutoScalingGroups(asgName);
        if (asgs.isEmpty()) {
            LOGGER.error(String.format("Not found ASG with name %s", asgName));
            return Collections.emptyList();
        } else {
            return asgs.get(0).getLoadBalancerNames();
        }
    }

    /**
     * Gets the list of availability zones for an ASG. Can be overridden in subclasses.
     * @param region the region
     * @param asgName the ASG name.
     * @return the list of the availability zones that the ASG has.
     */
    protected List<String> getAvailabilityZonesForAsg(String region, String asgName) {
        List<com.amazonaws.services.autoscaling.model.AutoScalingGroup> asgs =
                getAwsClient(region).describeAutoScalingGroups(asgName);
        if (asgs.isEmpty()) {
            LOGGER.error(String.format("Not found ASG with name %s", asgName));
            return null;
        } else {
            return asgs.get(0).getAvailabilityZones();
        }
    }

    /**
     * Gets the list of availability zones for a load balancer. Can be overridden in subclasses.
     * @param region the region
     * @param lbName the load balancer name.
     * @return the list of the availability zones that the load balancer has.
     */
    protected List<String> getAvailabilityZonesForLoadBalancer(String region, String lbName) {
        List<LoadBalancerDescription> lbs =
                getAwsClient(region).describeElasticLoadBalancers(lbName);
        if (lbs.isEmpty()) {
            LOGGER.error(String.format("Not found load balancer with name %s", lbName));
            return null;
        } else {
            return lbs.get(0).getAvailabilityZones();
        }
    }

    private AWSClient getAwsClient(String region) {
        AWSClient awsClient = regionToAwsClient.get(region);
        if (awsClient == null) {
            awsClient = new AWSClient(region, awsCredentialsProvider);
            regionToAwsClient.put(region, awsClient);
        }
        return awsClient;
    }


    private boolean haveSameZones(List<String> zones1, List<String> zones2) {
        if (zones1 == null || zones2 == null) {
            return true;
        }
        if (zones1.size() != zones1.size()) {
            return false;
        }
        for (String zone : zones1) {
            if (!zones2.contains(zone)) {
                return false;
            }
        }
        for (String zone : zones2) {
            if (!zones1.contains(zone)) {
                return false;
            }
        }
        return true;
    }
}

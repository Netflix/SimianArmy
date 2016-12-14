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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.netflix.simianarmy.client.MonkeyRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerAttributes;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.conformity.AutoScalingGroup;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.Conformity;
import com.netflix.simianarmy.conformity.ConformityRule;

/**
 * The class implementing a conformity rule that checks if the cross-zone load balancing is enabled 
 * for all cluster ELBs.
 */
public class CrossZoneLoadBalancing implements ConformityRule {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrossZoneLoadBalancing.class);
  
    private final Map<String, AWSClient> regionToAwsClient = Maps.newHashMap();
  
    private AWSCredentialsProvider awsCredentialsProvider;
  
    private static final String RULE_NAME = "CrossZoneLoadBalancing";
    private static final String REASON = "Cross-zone load balancing is disabled";
  
    /**
     * Constructs an instance with the default AWS credentials provider chain.
     * @see com.amazonaws.auth.DefaultAWSCredentialsProviderChain
     */
    public CrossZoneLoadBalancing() {
        this(new DefaultAWSCredentialsProviderChain());
    }
  
    /**
     * Constructs an instance with the passed AWS Credential Provider.
     * @param awsCredentialsProvider
     */
    public CrossZoneLoadBalancing(AWSCredentialsProvider awsCredentialsProvider) {
        this.awsCredentialsProvider = awsCredentialsProvider;
    }
    
    @Override
    public Conformity check(Cluster cluster) {
        Collection<String> failedComponents = Lists.newArrayList();
        for (AutoScalingGroup asg : cluster.getAutoScalingGroups()) {
            try {
                for (String lbName : getLoadBalancerNamesForAsg(cluster.getRegion(), asg.getName())) {
                    if (!isCrossZoneLoadBalancingEnabled(cluster.getRegion(), lbName)) {
                        LOGGER.info(String.format("ELB %s in %s does not have cross-zone load balancing enabled",
                                    lbName, cluster.getRegion()));
                        failedComponents.add(lbName);
                    }
                }
            } catch (MonkeyRestClient.DataReadException e) {
                LOGGER.error(String.format("Transient error reading ELB for %s in %s - skipping this check",
                             asg.getName(), cluster.getRegion()), e);
            }
        }
        return new Conformity(getName(), failedComponents);
    }
  
    /**
     * Gets the cross-zone load balancing option for an ELB. Can be overridden in subclasses.
     * @param region the region
     * @param lbName the ELB name
     * @return {@code true} if cross-zone load balancing is enabled
     */
    protected boolean isCrossZoneLoadBalancingEnabled(String region, String lbName) {
        LoadBalancerAttributes attrs = getAwsClient(region).describeElasticLoadBalancerAttributes(lbName);
        return attrs.getCrossZoneLoadBalancing().isEnabled();
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

    private AWSClient getAwsClient(String region) {
        AWSClient awsClient = regionToAwsClient.get(region);
        if (awsClient == null) {
            awsClient = new AWSClient(region, awsCredentialsProvider);
            regionToAwsClient.put(region, awsClient);
        }
        return awsClient;
    }


    
}

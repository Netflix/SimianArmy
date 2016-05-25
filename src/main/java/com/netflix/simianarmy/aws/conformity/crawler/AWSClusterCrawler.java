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
package com.netflix.simianarmy.aws.conformity.crawler;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.TagDescription;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.SuspendedProcess;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.ClusterCrawler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The class implementing a crawler that gets the auto scaling groups from AWS.
 */
public class AWSClusterCrawler implements ClusterCrawler {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AWSClusterCrawler.class);

    private static final String NS = "simianarmy.conformity.cluster";

    /** The map from region to the aws client in the region. */
    private final Map<String, AWSClient> regionToAwsClient = Maps.newHashMap();

    private final MonkeyConfiguration cfg;

    /**
     * Instantiates a new cluster crawler.
     *
     * @param regionToAwsClient
     *            the map from region to the corresponding aws client for the region
     */
    public AWSClusterCrawler(Map<String, AWSClient> regionToAwsClient, MonkeyConfiguration cfg) {
        Validate.notNull(regionToAwsClient);
        Validate.notNull(cfg);
        for (Map.Entry<String, AWSClient> entry : regionToAwsClient.entrySet()) {
            this.regionToAwsClient.put(entry.getKey(), entry.getValue());
        }
        this.cfg = cfg;
    }

    /**
     * In this implementation, every auto scaling group is considered a cluster.
     * @param clusterNames
     *          the cluster names
     * @return the list of clusters matching the names, when names are empty, return all clusters
     */
    @Override
    public List<Cluster> clusters(String... clusterNames) {
        List<Cluster> list = Lists.newArrayList();
        for (Map.Entry<String, AWSClient> entry : regionToAwsClient.entrySet()) {
            String region = entry.getKey();
            AWSClient awsClient = entry.getValue();
            Set<String> asgInstances = Sets.newHashSet();
            LOGGER.info(String.format("Crawling clusters in region %s", region));
            for (AutoScalingGroup asg : awsClient.describeAutoScalingGroups(clusterNames)) {
                List<String> instances = Lists.newArrayList();
                for (Instance instance : asg.getInstances()) {
                    instances.add(instance.getInstanceId());
                    asgInstances.add(instance.getInstanceId());
                }
                com.netflix.simianarmy.conformity.AutoScalingGroup conformityAsg =
                        new com.netflix.simianarmy.conformity.AutoScalingGroup(
                                asg.getAutoScalingGroupName(),
                                instances.toArray(new String[instances.size()]));

                for (SuspendedProcess sp : asg.getSuspendedProcesses()) {
                    if ("AddToLoadBalancer".equals(sp.getProcessName())) {
                        LOGGER.info(String.format("ASG %s is suspended: %s", asg.getAutoScalingGroupName(),
                                asg.getSuspendedProcesses()));
                        conformityAsg.setSuspended(true);
                    }
                }

                Cluster cluster = new Cluster(asg.getAutoScalingGroupName(), region, conformityAsg);
                
                List<TagDescription> tagDescriptions = asg.getTags();
                for (TagDescription tagDescription : tagDescriptions) {
                    if ( BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY.equalsIgnoreCase(tagDescription.getKey()) ) {
                        String value = tagDescription.getValue();
                        if (value != null) {
                            cluster.setOwnerEmail(value);
                        }
                    }
                }

                updateCluster(cluster);
                list.add(cluster);
            }
            //Cluster containing all solo instances
            Set<String> instances = Sets.newHashSet();
            for (com.amazonaws.services.ec2.model.Instance awsInstance : awsClient.describeInstances()) {
                if (!asgInstances.contains(awsInstance.getInstanceId())) {
                    LOGGER.info(String.format("Adding instance %s to soloInstances cluster.",
                            awsInstance.getInstanceId()));
                    instances.add(awsInstance.getInstanceId());
                }
            }
            //Only create cluster if we have solo instances.
            if (!instances.isEmpty()) {
                Cluster cluster = new Cluster("SoloInstances", region, instances);
                updateCluster(cluster);
                list.add(cluster);
            }
        }
        return list;
    }

    private void updateCluster(Cluster cluster) {
        updateExcludedConformityRules(cluster);
        cluster.setOwnerEmail(getOwnerEmailForCluster(cluster));
        String prop = String.format("simianarmy.conformity.cluster.%s.optedOut", cluster.getName());
        if (cfg.getBoolOrElse(prop, false)) {
            LOGGER.info(String.format("Cluster %s is opted out of Conformity Monkey.", cluster.getName()));
            cluster.setOptOutOfConformity(true);
        } else {
            cluster.setOptOutOfConformity(false);
        }
    }

    /**
     * Gets the owner email from the monkey configuration.
     * @param cluster
     *          the cluster
     * @return the owner email if it is defined in the configuration, null otherwise.
     */
    @Override
    public String getOwnerEmailForCluster(Cluster cluster) {
        String prop = String.format("%s.%s.ownerEmail", NS, cluster.getName());
        String ownerEmail = cfg.getStr(prop);
        if (ownerEmail == null) {
            ownerEmail = cluster.getOwnerEmail();
            if (ownerEmail == null) {
                LOGGER.info(String.format("No owner email is found for cluster %s in configuration "
                    + "%s or tag %s.", cluster.getName(), prop, BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY));
            } else {
                LOGGER.info(String.format("Found owner email %s for cluster %s in tag %s.",
                        ownerEmail, cluster.getName(), BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY));
                return ownerEmail;
            }
        } else {
            LOGGER.info(String.format("Found owner email %s for cluster %s in configuration %s.",
                    ownerEmail, cluster.getName(), prop));
        }
        return ownerEmail;
    }

    @Override
    public void updateExcludedConformityRules(Cluster cluster) {
        String prop = String.format("%s.%s.excludedRules", NS, cluster.getName());
        String excludedRules = cfg.getStr(prop);
        if (StringUtils.isNotBlank(excludedRules)) {
            LOGGER.info(String.format("Excluded rules for cluster %s are : %s", cluster.getName(), excludedRules));
            cluster.excludeRules(StringUtils.split(excludedRules, ","));
        }
    }
}

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
package com.netflix.simianarmy.basic.conformity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.ClusterCrawler;
import com.netflix.simianarmy.conformity.ConformityClusterTracker;
import com.netflix.simianarmy.conformity.ConformityEmailNotifier;
import com.netflix.simianarmy.conformity.ConformityMonkey;
import com.netflix.simianarmy.conformity.ConformityRuleEngine;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The basic implementation of Conformity Monkey. */
public class BasicConformityMonkey extends ConformityMonkey {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicConformityMonkey.class);

    /** The Constant NS. */
    private static final String NS = "simianarmy.conformity.";

    /** The cfg. */
    private final MonkeyConfiguration cfg;

    private final ClusterCrawler crawler;

    private final ConformityEmailNotifier emailNotifier;

    private final Collection<String> regions = Lists.newArrayList();

    private final ConformityClusterTracker clusterTracker;

    private final MonkeyCalendar calendar;

    private final ConformityRuleEngine ruleEngine;

    /** Flag to indicate whether the monkey is leashed. */
    private boolean leashed;

    /**
     * Clusters that are not conforming in the last check.
     */
    private final Map<String, Collection<Cluster>> nonconformingClusters = Maps.newHashMap();

    /**
     * Clusters that are conforming in the last check.
     */
    private final Map<String, Collection<Cluster>> conformingClusters = Maps.newHashMap();

    /**
     * Clusters that the monkey failed to check for some reason.
     */
    private final Map<String, Collection<Cluster>> failedClusters = Maps.newHashMap();

    /**
     * Clusters that do not exist in the cloud anymore.
     */
    private final Map<String, Collection<Cluster>> nonexistentClusters = Maps.newHashMap();

    /**
     * Instantiates a new basic conformity monkey.
     *
     * @param ctx
     *            the ctx
     */
    public BasicConformityMonkey(Context ctx) {
        super(ctx);
        cfg = ctx.configuration();
        crawler = ctx.clusterCrawler();
        ruleEngine = ctx.ruleEngine();
        emailNotifier = ctx.emailNotifier();
        for (String region : ctx.regions()) {
            regions.add(region);
        }
        clusterTracker = ctx.clusterTracker();
        calendar = ctx.calendar();
        leashed = ctx.isLeashed();
    }

    /** {@inheritDoc} */
    @Override
    public void doMonkeyBusiness() {
        cfg.reload();
        context().resetEventReport();

        if (isConformityMonkeyEnabled()) {
            nonconformingClusters.clear();
            conformingClusters.clear();
            failedClusters.clear();
            nonexistentClusters.clear();

            List<Cluster> clusters = crawler.clusters();
            Map<String, Set<String>> existingClusterNamesByRegion = Maps.newHashMap();
            for (String region : regions) {
                existingClusterNamesByRegion.put(region, new HashSet<String>());
            }
            for (Cluster cluster : clusters) {
                existingClusterNamesByRegion.get(cluster.getRegion()).add(cluster.getName());
            }
            List<Cluster> trackedClusters = clusterTracker.getAllClusters(regions.toArray(new String[regions.size()]));
            for (Cluster trackedCluster : trackedClusters) {
                if (!existingClusterNamesByRegion.get(trackedCluster.getRegion()).contains(trackedCluster.getName())) {
                    addCluster(nonexistentClusters, trackedCluster);
                }
            }
            for (String region : regions) {
                Collection<Cluster> toDelete = nonexistentClusters.get(region);
                if (toDelete != null) {
                    clusterTracker.deleteClusters(toDelete.toArray(new Cluster[toDelete.size()]));
                }
            }

            LOGGER.info(String.format("Performing conformity check for %d crawled clusters.", clusters.size()));
            Date now = calendar.now().getTime();
            for (Cluster cluster : clusters) {
                boolean conforming;
                try {
                    conforming = ruleEngine.check(cluster);
                } catch (Exception e) {
                    LOGGER.error(String.format("Failed to perform conformity check for cluster %s", cluster.getName()),
                            e);
                    addCluster(failedClusters, cluster);
                    continue;
                }
                cluster.setUpdateTime(now);
                cluster.setConforming(conforming);
                if (conforming) {
                    LOGGER.info(String.format("Cluster %s is conforming", cluster.getName()));
                    addCluster(conformingClusters, cluster);
                } else {
                    LOGGER.info(String.format("Cluster %s is not conforming", cluster.getName()));
                    addCluster(nonconformingClusters, cluster);
                }
                if (!leashed) {
                    LOGGER.info(String.format("Saving cluster %s", cluster.getName()));
                    clusterTracker.addOrUpdate(cluster);
                } else {
                    LOGGER.info(String.format(
                            "The conformity monkey is leashed, no data change is made for cluster %s.",
                            cluster.getName()));
                }
            }
            if (!leashed) {
                emailNotifier.sendNotifications();
            } else {
                LOGGER.info("Conformity monkey is leashed, no notification is sent.");
            }
            if (cfg.getBoolOrElse(NS + "summaryEmail.enabled", true)) {
                sendConformitySummaryEmail();
            }
        }
    }

    private static void addCluster(Map<String, Collection<Cluster>> map, Cluster cluster) {
        Collection<Cluster> clusters = map.get(cluster.getRegion());
        if (clusters == null) {
            clusters = Lists.newArrayList();
            map.put(cluster.getRegion(), clusters);
        }
        clusters.add(cluster);
    }

    /**
     * Send a summary email with about the last run of the conformity monkey.
     */
    protected void sendConformitySummaryEmail() {
        String summaryEmailTarget = cfg.getStr(NS + "summaryEmail.to");
        if (!StringUtils.isEmpty(summaryEmailTarget)) {
            if (!emailNotifier.isValidEmail(summaryEmailTarget)) {
                LOGGER.error(String.format("The email target address '%s' for Conformity summary email is invalid",
                        summaryEmailTarget));
                return;
            }
            StringBuilder message = new StringBuilder();
            for (String region : regions) {
                appendSummary(message, "nonconforming", nonconformingClusters, region, true);
                appendSummary(message, "failed to check", failedClusters, region, true);
                appendSummary(message, "nonexistent", nonexistentClusters, region, true);
                appendSummary(message, "conforming", conformingClusters, region, false);
            }
            String subject = getSummaryEmailSubject();
            emailNotifier.sendEmail(summaryEmailTarget, subject, message.toString());
        }
    }

    private void appendSummary(StringBuilder message, String summaryName,
                               Map<String, Collection<Cluster>> regionToClusters, String region, boolean showDetails) {
        Collection<Cluster> clusters = regionToClusters.get(region);
        if (clusters == null) {
            clusters = Lists.newArrayList();
        }
        message.append(String.format("Total %s clusters = %d in region %s<br/>",
                summaryName, clusters.size(), region));
        if (showDetails) {
            List<String> clusterNames = Lists.newArrayList();
            for (Cluster cluster : clusters) {
                clusterNames.add(cluster.getName());
            }
            message.append(String.format("List: %s<br/><br/>", StringUtils.join(clusterNames, ",")));
        }
    }

    /**
     * Gets the summary email subject for the last run of conformity monkey.
     * @return the subject of the summary email
     */
    protected String getSummaryEmailSubject() {
        return String.format("Conformity monkey execution summary (%s)", StringUtils.join(regions, ","));
    }

    private boolean isConformityMonkeyEnabled() {
        String prop = NS + "enabled";
        if (cfg.getBoolOrElse(prop, true)) {
            return true;
        }
        LOGGER.info("Conformity Monkey is disabled, set {}=true", prop);
        return false;
    }
}

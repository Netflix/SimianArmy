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
package com.netflix.simianarmy.conformity;

import java.util.List;

/**
 * The interface of the crawler for Conformity Monkey to get the cluster information.
 */
public interface ClusterCrawler {

    /**
     * Gets the up to date information for a collection of clusters. When the input argument is null
     * or empty, the method returns all clusters.
     *
     * @param clusterNames
     *          the cluster names
     * @return the list of clusters
     */
    List<Cluster> clusters(String... clusterNames);

    /**
     * Gets the owner email for a cluster to set the ownerEmail field when crawl.
     * @param cluster
     *          the cluster
     * @return the owner email of the cluster
     */
    String getOwnerEmailForCluster(Cluster cluster);

    /**
     * Updates the excluded conformity rules for the given cluster.
     * @param cluster
     */
    void updateExcludedConformityRules(Cluster cluster);
}

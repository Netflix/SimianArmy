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
 * The interface that defines the tracker to manage clusters for Conformity monkey to use.
 */
public interface ConformityClusterTracker {
    /**
     * Adds a cluster to the tracker. If the cluster with the same name already exists,
     * the method updates the record with the cluster parameter.
     * @param cluster
     *          the cluster to add or update
     */
    void addOrUpdate(Cluster cluster);

    /**
     * Gets the list of clusters in a list of regions.
     * @param regions
     *      the regions of the clusters, when the parameter is null or empty, the method returns
     *      clusters from all regions
     * @return list of clusters in the given regions
     */
    List<Cluster> getAllClusters(String... regions);

    /**
     * Gets the list of non-conforming clusters in a list of regions.
     * @param regions the regions of the clusters, when the parameter is null or empty, the method returns
     * clusters from all regions
     * @return list of clusters in the given regions
     */
    List<Cluster> getNonconformingClusters(String... regions);

    /**
     * Gets the cluster with a specific name from .
     * @param name the cluster name
     * @param region the region of the cluster
     * @return the cluster with the name
     */
    Cluster getCluster(String name, String region);


    /**
     * Deletes a list of clusters from the tracker.
     * @param clusters the list of clusters to delete. The parameter cannot be null. If it is empty,
     *                 no cluster is deleted.
     */
    void deleteClusters(Cluster... clusters);

}

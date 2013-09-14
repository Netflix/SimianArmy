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
package com.netflix.simianarmy;

import java.util.List;
import java.util.Map;

import org.jclouds.compute.ComputeService;

/**
 * The CloudClient interface. This abstractions provides the interface that the monkeys need to interact with
 * "the cloud".
 */
public interface CloudClient {

    /**
     * Terminates instance.
     *
     * @param instanceId
     *            the instance id
     *
     * @throws NotFoundException
     *             if the instance no longer exists or was already terminated after the crawler discovered it then you
     *             should get a NotFoundException
     */
    void terminateInstance(String instanceId);

    /**
     * Deletes an auto scaling group.
     *
     * @param asgName
     *          the auto scaling group name
     */
    void deleteAutoScalingGroup(String asgName);

    /**
     * Deletes a launch configuration.
     *
     * @param launchConfigName
     *          the launch configuration name
     */
    void deleteLaunchConfiguration(String launchConfigName);

    /**
     * Deletes a volume.
     *
     * @param volumeId
     *          the volume id
     */
    void deleteVolume(String volumeId);

    /**
     * Deletes a snapshot.
     *
     * @param snapshotId
     *          the snapshot id.
     */
    void deleteSnapshot(String snapshotId);

    /** Deletes an image.
     *
     * @param imageId
     *          the image id.
     */
     void deleteImage(String imageId);

     /**
     * Adds or overwrites tags for the specified resources.
     *
     * @param keyValueMap
     *          the new tags in the form of map from key to value
     *
     * @param resourceIds
     *          the list of resource ids
     */
    void createTagsForResources(Map<String, String> keyValueMap, String... resourceIds);

    /**
     * Lists all EBS volumes attached to the specified instance.
     *
     * @param instanceId
     *            the instance id
     * @param includeRoot
     *            if the root volume is on EBS, should we include it?
     *
     * @throws NotFoundException
     *             if the instance no longer exists or was already terminated after the crawler discovered it then you
     *             should get a NotFoundException
     */
    List<String> listAttachedVolumes(String instanceId, boolean includeRoot);

    /**
     * Detaches an EBS volumes from the specified instance.
     *
     * @param instanceId
     *            the instance id
     * @param volumeId
     *            the volume id
     * @param force
     *            if we should force-detach the volume.  Probably best not to use on high-value volumes.
     *
     * @throws NotFoundException
     *             if the instance no longer exists or was already terminated after the crawler discovered it then you
     *             should get a NotFoundException
     */
    void detachVolume(String instanceId, String volumeId, boolean force);

    /**
     * Returns the jClouds compute service.
     */
    ComputeService getJcloudsComputeService();

    /**
     * Returns the jClouds node id for an instance id on this CloudClient.
     */
    String getJcloudsId(String instanceId);
}

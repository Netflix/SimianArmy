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

import org.jclouds.compute.ComputeService;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.ssh.SshClient;

import java.util.List;
import java.util.Map;

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
     * Deletes an elastic load balancer.
     *
     * @param elbId
     *          the elastic load balancer id
     */
    void deleteElasticLoadBalancer(String elbId);

    /**
     * Deletes a DNS record.
     *
     * @param dnsName
     *          the DNS record to delete
     * @param dnsType
     *          the DNS type (CNAME, A, or AAAA)
     * @param hostedZoneID
     *          the ID of the hosted zone (required for AWS Route53 records)
     */
    public void deleteDNSRecord(String dnsName, String dnsType, String hostedZoneID);

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

    /**
     * Opens an SSH connection to an instance.
     *
     * @param instanceId
     *            instance id to connect to
     * @param credentials
     *            SSH credentials to use
     * @return {@link SshClient}, in connected state
     */
    SshClient connectSsh(String instanceId, LoginCredentials credentials);

    /**
     * Finds a security group with the given name, that can be applied to the given instance.
     *
     * For example, if it is a VPC instance, it makes sure that it is in the same VPC group.
     *
     * @param instanceId
     *            the instance that the group must be applied to
     * @param groupName
     *            the name of the group to find
     *
     * @return The group id, or null if not found
     */
    String findSecurityGroup(String instanceId, String groupName);

    /**
     * Creates an (empty) security group, that can be applied to the given instance.
     *
     * @param instanceId
     *            instance that group should be applicable to
     * @param groupName
     *            name for new group
     * @param description
     *            description for new group
     *
     * @return the id of the security group
     */
    String createSecurityGroup(String instanceId, String groupName, String description);

    /**
     * Checks if we can change the security groups of an instance.
     *
     * @param instanceId
     *            instance to check
     *
     * @return true iff we can change security groups.
     */
    boolean canChangeInstanceSecurityGroups(String instanceId);

    /**
     * Sets the security groups for an instance.
     *
     * Note this is only valid for VPC instances.
     *
     * @param instanceId
     *            the instance id
     * @param groupIds
     *            ids of desired new groups
     *
     * @throws NotFoundException
     *             if the instance no longer exists or was already terminated after the crawler discovered it then you
     *             should get a NotFoundException
     */
    void setInstanceSecurityGroups(String instanceId, List<String> groupIds);
}

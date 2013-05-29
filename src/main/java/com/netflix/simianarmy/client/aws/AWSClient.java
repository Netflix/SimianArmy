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
package com.netflix.simianarmy.client.aws;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsResult;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteSnapshotRequest;
import com.amazonaws.services.ec2.model.DeleteVolumeRequest;
import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeSnapshotsRequest;
import com.amazonaws.services.ec2.model.DescribeSnapshotsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.Volume;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.netflix.simianarmy.CloudClient;
import com.netflix.simianarmy.NotFoundException;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * The Class AWSClient. Simple Amazon EC2 and Amazon ASG client interface.
 */
public class AWSClient implements CloudClient {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AWSClient.class);

    /** The region. */
    private final String region;

    private final AWSCredentialsProvider awsCredentialsProvider;

    /**
     * This constructor will let the AWS SDK obtain the credentials, which will
     * choose such in the following order:
     *
     * <ul>
     * <li>Environment Variables: {@code AWS_ACCESS_KEY_ID} and
     * {@code AWS_SECRET_KEY}</li>
     * <li>Java System Properties: {@code aws.accessKeyId} and
     * {@code aws.secretKey}</li>
     * <li>Instance Metadata Service, which provides the credentials associated
     * with the IAM role for the EC2 instance</li>
     * </ul>
     *
     * <p>
     * If credentials are provided explicitly, use
     * {@link com.netflix.simianarmy.basic.BasicSimianArmyContext#exportCredentials(String, String)}
     * which will set them as System properties used by each AWS SDK call.
     * </p>
     *
     * <p>
     * <b>Note:</b> Avoid storing credentials received dynamically via the
     * {@link com.amazonaws.auth.InstanceProfileCredentialsProvider} as these will be rotated and
     * their renewal is handled by its
     * {@link com.amazonaws.auth.InstanceProfileCredentialsProvider#getCredentials()} method.
     * </p>
     *
     * @param region
     *            the region
     * @see com.amazonaws.auth.DefaultAWSCredentialsProviderChain
     * @see com.amazonaws.auth.InstanceProfileCredentialsProvider
     * @see com.netflix.simianarmy.basic.BasicSimianArmyContext#exportCredentials(String, String)
     */
    public AWSClient(String region) {
        this.region = region;
        this.awsCredentialsProvider = null;
    }

    /**
     * The constructor allows you to provide your own AWS credentials provider.
     * @param region
     *          the region
     * @param awsCredentialsProvider
     *          the AWS credentials provider
     */
    public AWSClient(String region, AWSCredentialsProvider awsCredentialsProvider) {
        this.region = region;
        this.awsCredentialsProvider = awsCredentialsProvider;
    }

    /**
     * The Region.
     *
     * @return the region the client is configured to communicate with
     */
    public String region() {
        return region;
    }

    /**
     * Amazon EC2 client. Abstracted to aid testing.
     *
     * @return the Amazon EC2 client
     */
    protected AmazonEC2 ec2Client() {
        AmazonEC2 client;
        if (awsCredentialsProvider == null) {
            client = new AmazonEC2Client();
        } else {
            client = new AmazonEC2Client(awsCredentialsProvider);
        }
        client.setEndpoint("ec2." + region + ".amazonaws.com");
        return client;
    }

    /**
     * Amazon ASG client. Abstracted to aid testing.
     *
     * @return the Amazon Auto Scaling client
     */
    protected AmazonAutoScalingClient asgClient() {
        AmazonAutoScalingClient client;
        if (awsCredentialsProvider == null) {
            client = new AmazonAutoScalingClient();
        } else {
            client = new AmazonAutoScalingClient(awsCredentialsProvider);
        }
        client.setEndpoint("autoscaling." + region + ".amazonaws.com");
        return client;
    }

    /**
     * Amazon ELB client. Abstracted to aid testing.
     *
     * @return the Amazon ELB client
     */
    protected AmazonElasticLoadBalancingClient elbClient() {
        AmazonElasticLoadBalancingClient client;
        if (awsCredentialsProvider == null) {
            client = new AmazonElasticLoadBalancingClient();
        } else {
            client = new AmazonElasticLoadBalancingClient(awsCredentialsProvider);
        }
        client.setEndpoint("elasticloadbalancing." + region + ".amazonaws.com");
        return client;
    }

    /**
     * Amazon SimpleDB client.
     *
     * @return the Amazon SimpleDB client
     */
    public AmazonSimpleDB sdbClient() {
        AmazonSimpleDB client;
        if (awsCredentialsProvider == null) {
            client = new AmazonSimpleDBClient();
        } else {
            client = new AmazonSimpleDBClient(awsCredentialsProvider);
        }
        // us-east-1 has special naming
        // http://docs.amazonwebservices.com/general/latest/gr/rande.html#sdb_region
        if (region == null || region.equals("us-east-1")) {
            client.setEndpoint("sdb.amazonaws.com");
        } else {
            client.setEndpoint("sdb." + region + ".amazonaws.com");
        }
        return client;
    }

    /**
     * Describe auto scaling groups.
     *
     * @return the list
     */
    public List<AutoScalingGroup> describeAutoScalingGroups() {
        return describeAutoScalingGroups((String[]) null);
    }

    /**
     * Describe a set of specific auto scaling groups.
     *
     * @param names the ASG names
     * @return the auto scaling groups
     */
    public List<AutoScalingGroup> describeAutoScalingGroups(String... names) {
        if (names == null || names.length == 0) {
            LOGGER.info(String.format("Getting all auto-scaling groups in region %s.", region));
        } else {
            LOGGER.info(String.format("Getting auto-scaling groups for %d names in region %s.", names.length, region));
        }

        List<AutoScalingGroup> asgs = new LinkedList<AutoScalingGroup>();

        AmazonAutoScalingClient asgClient = asgClient();
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        if (names != null) {
            request.setAutoScalingGroupNames(Arrays.asList(names));
        }
        DescribeAutoScalingGroupsResult result = asgClient.describeAutoScalingGroups(request);

        asgs.addAll(result.getAutoScalingGroups());
        while (result.getNextToken() != null) {
            request.setNextToken(result.getNextToken());
            result = asgClient.describeAutoScalingGroups(request);
            asgs.addAll(result.getAutoScalingGroups());
        }

        LOGGER.info(String.format("Got %d auto-scaling groups in region %s.", asgs.size(), region));
        return asgs;
    }

    /**
     * Describe a set of specific ELBs.
     *
     * @param names the ELB names
     * @return the ELBs
     */
    public List<LoadBalancerDescription> describeElasticLoadBalancers(String... names) {
        if (names == null || names.length == 0) {
            LOGGER.info(String.format("Getting all ELBs in region %s.", region));
        } else {
            LOGGER.info(String.format("Getting ELBs for %d names in region %s.", names.length, region));
        }

        AmazonElasticLoadBalancingClient elbClient = elbClient();
        DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest().withLoadBalancerNames(names);
        DescribeLoadBalancersResult result = elbClient.describeLoadBalancers(request);
        List<LoadBalancerDescription> elbs = result.getLoadBalancerDescriptions();
        LOGGER.info(String.format("Got %d ELBs in region %s.", elbs.size(), region));
        return elbs;
    }


    /**
     * Describe a set of specific auto-scaling instances.
     *
     * @param instanceIds the instance ids
     * @return the instances
     */
    public List<AutoScalingInstanceDetails> describeAutoScalingInstances(String... instanceIds) {
        if (instanceIds == null || instanceIds.length == 0) {
            LOGGER.info(String.format("Getting all auto-scaling instances in region %s.", region));
        } else {
            LOGGER.info(String.format("Getting auto-scaling instances for %d ids in region %s.",
                    instanceIds.length, region));
        }

        List<AutoScalingInstanceDetails> instances = new LinkedList<AutoScalingInstanceDetails>();

        AmazonAutoScalingClient asgClient = asgClient();
        DescribeAutoScalingInstancesRequest request = new DescribeAutoScalingInstancesRequest();
        if (instanceIds != null) {
            request.setInstanceIds(Arrays.asList(instanceIds));
        }
        DescribeAutoScalingInstancesResult result = asgClient.describeAutoScalingInstances(request);

        instances.addAll(result.getAutoScalingInstances());
        while (result.getNextToken() != null) {
            request = request.withNextToken(result.getNextToken());
            result = asgClient.describeAutoScalingInstances(request);
            instances.addAll(result.getAutoScalingInstances());
        }

        LOGGER.info(String.format("Got %d auto-scaling instances.", instances.size()));
        return instances;
    }

    /**
     * Describe a set of specific instances.
     *
     * @param instanceIds the instance ids
     * @return the instances
     */
    public List<Instance> describeInstances(String... instanceIds) {
        if (instanceIds == null || instanceIds.length == 0) {
            LOGGER.info(String.format("Getting all EC2 instances in region %s.", region));
        } else {
            LOGGER.info(String.format("Getting EC2 instances for %d ids in region %s.", instanceIds.length, region));
        }

        List<Instance> instances = new LinkedList<Instance>();

        AmazonEC2 ec2Client = ec2Client();
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        if (instanceIds != null) {
            request.withInstanceIds(Arrays.asList(instanceIds));
        }
        DescribeInstancesResult result = ec2Client.describeInstances(request);
        for (Reservation reservation : result.getReservations()) {
            instances.addAll(reservation.getInstances());
        }

        LOGGER.info(String.format("Got %d EC2 instances in region %s.", instances.size(), region));
        return instances;
    }

    /**
     * Describe a set of specific launch configurations.
     *
     * @param names the launch configuration names
     * @return the launch configurations
     */
    public List<LaunchConfiguration> describeLaunchConfigurations(String... names) {
        if (names == null || names.length == 0) {
            LOGGER.info(String.format("Getting all launch configurations in region %s.", region));
        } else {
            LOGGER.info(String.format("Getting launch configurations for %d names in region %s.",
                    names.length, region));
        }

        List<LaunchConfiguration> lcs = new LinkedList<LaunchConfiguration>();

        AmazonAutoScalingClient asgClient = asgClient();
        DescribeLaunchConfigurationsRequest request = new DescribeLaunchConfigurationsRequest()
        .withLaunchConfigurationNames(names);
        DescribeLaunchConfigurationsResult result = asgClient.describeLaunchConfigurations(request);

        lcs.addAll(result.getLaunchConfigurations());
        while (result.getNextToken() != null) {
            request.setNextToken(result.getNextToken());
            result = asgClient.describeLaunchConfigurations(request);
            lcs.addAll(result.getLaunchConfigurations());
        }

        LOGGER.info(String.format("Got %d launch configurations in region %s.", lcs.size(), region));
        return lcs;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAutoScalingGroup(String asgName) {
        Validate.notEmpty(asgName);
        LOGGER.info(String.format("Deleting auto-scaling group with name %s in region %s.", asgName, region));
        AmazonAutoScalingClient asgClient = asgClient();
        DeleteAutoScalingGroupRequest request = new DeleteAutoScalingGroupRequest()
        .withAutoScalingGroupName(asgName);
        asgClient.deleteAutoScalingGroup(request);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteLaunchConfiguration(String launchConfigName) {
        Validate.notEmpty(launchConfigName);
        LOGGER.info(String.format("Deleting launch configuration with name %s in region %s.",
                launchConfigName, region));
        AmazonAutoScalingClient asgClient = asgClient();
        DeleteLaunchConfigurationRequest request = new DeleteLaunchConfigurationRequest()
                .withLaunchConfigurationName(launchConfigName);
        asgClient.deleteLaunchConfiguration(request);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteImage(String imageId) {
        Validate.notEmpty(imageId);
        LOGGER.info(String.format("Deleting image %s in region %s.",
                imageId, region));
        AmazonEC2 ec2Client = ec2Client();
        DeregisterImageRequest request = new DeregisterImageRequest(imageId);
        ec2Client.deregisterImage(request);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteVolume(String volumeId) {
        Validate.notEmpty(volumeId);
        LOGGER.info(String.format("Deleting volume %s in region %s.", volumeId, region));
        AmazonEC2 ec2Client = ec2Client();
        DeleteVolumeRequest request = new DeleteVolumeRequest().withVolumeId(volumeId);
        ec2Client.deleteVolume(request);
    }

    /** {@inheritDoc} */
    @Override
    public void deleteSnapshot(String snapshotId) {
        Validate.notEmpty(snapshotId);
        LOGGER.info(String.format("Deleting snapshot %s in region %s.", snapshotId, region));
        AmazonEC2 ec2Client = ec2Client();
        DeleteSnapshotRequest request = new DeleteSnapshotRequest().withSnapshotId(snapshotId);
        ec2Client.deleteSnapshot(request);
    }

    /** {@inheritDoc} */
    @Override
    public void terminateInstance(String instanceId) {
        Validate.notEmpty(instanceId);
        LOGGER.info(String.format("Terminating instance %s in region %s.", instanceId, region));
        try {
            ec2Client().terminateInstances(new TerminateInstancesRequest(Arrays.asList(instanceId)));
        } catch (AmazonServiceException e) {
            if (e.getErrorCode().equals("InvalidInstanceID.NotFound")) {
                throw new NotFoundException("AWS instance " + instanceId + " not found", e);
            }
            throw e;
        }
    }

    /**
     * Describe a set of specific EBS volumes.
     *
     * @param volumeIds the volume ids
     * @return the volumes
     */
    public List<Volume> describeVolumes(String... volumeIds) {
        if (volumeIds == null || volumeIds.length == 0) {
            LOGGER.info(String.format("Getting all EBS volumes in region %s.", region));
        } else {
            LOGGER.info(String.format("Getting EBS volumes for %d ids in region %s.", volumeIds.length, region));
        }

        AmazonEC2 ec2Client = ec2Client();
        DescribeVolumesRequest request = new DescribeVolumesRequest();
        if (volumeIds != null) {
            request.setVolumeIds(Arrays.asList(volumeIds));
        }
        DescribeVolumesResult result = ec2Client.describeVolumes(request);
        List<Volume> volumes = result.getVolumes();

        LOGGER.info(String.format("Got %d EBS volumes in region %s.", volumes.size(), region));
        return volumes;
    }

    /**
     * Describe a set of specific EBS snapshots.
     *
     * @param snapshotIds the snapshot ids
     * @return the snapshots
     */
    public List<Snapshot> describeSnapshots(String... snapshotIds) {
        if (snapshotIds == null || snapshotIds.length == 0) {
            LOGGER.info(String.format("Getting all EBS snapshots in region %s.", region));
        } else {
            LOGGER.info(String.format("Getting EBS snapshotIds for %d ids in region %s.", snapshotIds.length, region));
        }

        AmazonEC2 ec2Client = ec2Client();
        DescribeSnapshotsRequest request = new DescribeSnapshotsRequest();
        // Set the owner id to self to avoid getting snapshots from other accounts.
        request.withOwnerIds(Arrays.<String>asList("self"));
        if (snapshotIds != null) {
            request.setSnapshotIds(Arrays.asList(snapshotIds));
        }
        DescribeSnapshotsResult result = ec2Client.describeSnapshots(request);
        List<Snapshot> snapshots = result.getSnapshots();

        LOGGER.info(String.format("Got %d EBS snapshots in region %s.", snapshots.size(), region));
        return snapshots;
    }

    @Override
    public void createTagsForResources(Map<String, String> keyValueMap, String... resourceIds) {
        Validate.notNull(keyValueMap);
        Validate.notEmpty(keyValueMap);
        Validate.notNull(resourceIds);
        Validate.notEmpty(resourceIds);
        AmazonEC2 ec2Client = ec2Client();
        List<Tag> tags = new ArrayList<Tag>();
        for (Map.Entry<String, String> entry : keyValueMap.entrySet()) {
            tags.add(new Tag(entry.getKey(), entry.getValue()));
        }
        CreateTagsRequest req = new CreateTagsRequest(Arrays.asList(resourceIds), tags);
        ec2Client.createTags(req);
    }

    /**
     * Describe a set of specific images.
     *
     * @param imageIds the image ids
     * @return the images
     */
    public List<Image> describeImages(String... imageIds) {
        if (imageIds == null || imageIds.length == 0) {
            LOGGER.info(String.format("Getting all AMIs in region %s.", region));
        } else {
            LOGGER.info(String.format("Getting AMIs for %d ids in region %s.", imageIds.length, region));
        }

        AmazonEC2 ec2Client = ec2Client();
        DescribeImagesRequest request = new DescribeImagesRequest();
        if (imageIds != null) {
            request.setImageIds(Arrays.asList(imageIds));
        }
        DescribeImagesResult result = ec2Client.describeImages(request);
        List<Image> images = result.getImages();

        LOGGER.info(String.format("Got %d AMIs in region %s.", images.size(), region));
        return images;
    }
}

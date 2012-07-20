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
package com.netflix.simianarmy.aws;

import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.netflix.simianarmy.CloudClient;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;

/**
 * The Class AWSClient. Simple Amazon EC2 and Amazon ASG client interface.
 */
public class AWSClient implements CloudClient {

    /** The credential. */
    private AWSCredentials cred;

    /** The region. */
    private String region;

    /**
     * Instantiates a new AWS client.
     *
     * @param accessKey
     *            the access key
     * @param secretKey
     *            the secret key
     * @param region
     *            the region
     */
    public AWSClient(String accessKey, String secretKey, String region) {
        this.cred = new BasicAWSCredentials(accessKey, secretKey);
        this.region = region;
    }

    /**
     * Instantiates a new aWS client.
     *
     * @param cred
     *            the credential
     * @param region
     *            the region
     */
    public AWSClient(AWSCredentials cred, String region) {
        this.cred = cred;
        this.region = region;
    }

    /**
     * This constructor will use the {@link DefaultAWSCredentialsProviderChain} to obtain credentials.
     *
     * @param region
     *            the region
     */
    public AWSClient(String region) {
        this(new DefaultAWSCredentialsProviderChain().getCredentials(), region);
    }

    /**
     * Amazon EC2 client. Abstracted to aid testing.
     *
     * @return the amazon ec2 client
     */
    protected AmazonEC2 ec2Client() {
        AmazonEC2 client = new AmazonEC2Client(cred);
        client.setEndpoint("ec2." + region + ".amazonaws.com");
        return client;
    }

    /**
     * Amazon ASG client. Abstradted to aid testing.
     *
     * @return the amazon auto scaling client
     */
    protected AmazonAutoScalingClient asgClient() {
        AmazonAutoScalingClient client = new AmazonAutoScalingClient(cred);
        client.setEndpoint("autoscaling." + region + ".amazonaws.com");
        return client;
    }

    /**
     * Describe auto scaling groups.
     *
     * @return the list
     */
    public List<AutoScalingGroup> describeAutoScalingGroups() {
        List<AutoScalingGroup> asgs = new LinkedList<AutoScalingGroup>();

        AmazonAutoScalingClient asgClient = asgClient();
        DescribeAutoScalingGroupsRequest request = new DescribeAutoScalingGroupsRequest();
        DescribeAutoScalingGroupsResult result = asgClient.describeAutoScalingGroups(request);
        asgs.addAll(result.getAutoScalingGroups());
        while (result.getNextToken() != null) {
            request = request.withNextToken(result.getNextToken());
            result = asgClient.describeAutoScalingGroups(request);
            asgs.addAll(result.getAutoScalingGroups());
        }

        return asgs;
    }

    /** {@inheritDoc} */
    public void terminateInstance(String instanceId) {
        ec2Client().terminateInstances(new TerminateInstancesRequest(Arrays.asList(instanceId)));
    }
}

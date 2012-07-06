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

public class AWSClient implements CloudClient {
    private AWSCredentials cred;
    private String region;

    public AWSClient(String accessKey, String secretKey, String region) {
        this.cred = new BasicAWSCredentials(accessKey, secretKey);
        this.region = region;
    }

    public AWSClient(AWSCredentials cred, String region) {
        this.cred = cred;
        this.region = region;
    }

    protected AmazonEC2 ec2Client() {
        AmazonEC2 client = new AmazonEC2Client(cred);
        client.setEndpoint("ec2." + region + ".amazonaws.com");
        return client;
    }

    protected AmazonAutoScalingClient asgClient() {
        AmazonAutoScalingClient client = new AmazonAutoScalingClient(cred);
        client.setEndpoint("autoscaling" + region + ".amazonaws.com");
        return client;
    }

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

    public void terminateInstance(String instanceId) {
        ec2Client().terminateInstances(new TerminateInstancesRequest(Arrays.asList(instanceId)));
    }
}

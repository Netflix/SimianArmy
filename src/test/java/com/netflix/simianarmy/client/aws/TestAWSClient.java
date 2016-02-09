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
// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.client.aws;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestAWSClient extends AWSClient {
    public TestAWSClient() {
        super("us-east-1");
    }

    private AmazonEC2 ec2Mock = mock(AmazonEC2.class);

    protected AmazonEC2 ec2Client() {
        return ec2Mock;
    }

    private AmazonAutoScalingClient asgMock = mock(AmazonAutoScalingClient.class);

    protected AmazonAutoScalingClient asgClient() {
        return asgMock;
    }

    protected AmazonEC2 superEc2Client() {
        return super.ec2Client();
    }

    protected AmazonAutoScalingClient superAsgClient() {
        return super.asgClient();
    }

    @Test
    public void testClients() {
        TestAWSClient client1 = new TestAWSClient();
        Assert.assertNotNull(client1.superEc2Client(), "non null super ec2Client");
        Assert.assertNotNull(client1.superAsgClient(), "non null super asgClient");
    }

    @Test
    public void testTerminateInstance() {

        ArgumentCaptor<TerminateInstancesRequest> arg = ArgumentCaptor.forClass(TerminateInstancesRequest.class);

        this.terminateInstance("fake:i-12345678901234567");

        verify(ec2Mock).terminateInstances(arg.capture());

        List<String> instances = arg.getValue().getInstanceIds();
        Assert.assertEquals(instances.size(), 1);
        Assert.assertEquals(instances.get(0), "fake:i-12345678901234567");
    }

    private DescribeAutoScalingGroupsResult mkAsgResult(String asgName, String instanceId) {
        DescribeAutoScalingGroupsResult result = new DescribeAutoScalingGroupsResult();
        AutoScalingGroup asg = new AutoScalingGroup();
        asg.setAutoScalingGroupName(asgName);
        Instance inst = new Instance();
        inst.setInstanceId(instanceId);
        asg.setInstances(Arrays.asList(inst));
        result.setAutoScalingGroups(Arrays.asList(asg));
        return result;
    }

    @Test
    public void testDescribeAutoScalingGroups() {
        DescribeAutoScalingGroupsResult result1 = mkAsgResult("asg1", "i-123456789012345670");
        result1.setNextToken("nextToken");
        DescribeAutoScalingGroupsResult result2 = mkAsgResult("asg2", "i-123456789012345671");

        when(asgMock.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class))).thenReturn(result1)
                .thenReturn(result2);

        List<AutoScalingGroup> asgs = this.describeAutoScalingGroups();

        verify(asgMock, times(2)).describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class));

        Assert.assertEquals(asgs.size(), 2);

        // 2 asgs with 1 instance each
        Assert.assertEquals(asgs.get(0).getAutoScalingGroupName(), "asg1");
        Assert.assertEquals(asgs.get(0).getInstances().size(), 1);
        Assert.assertEquals(asgs.get(0).getInstances().get(0).getInstanceId(), "i-123456789012345670");

        Assert.assertEquals(asgs.get(1).getAutoScalingGroupName(), "asg2");
        Assert.assertEquals(asgs.get(1).getInstances().size(), 1);
        Assert.assertEquals(asgs.get(1).getInstances().get(0).getInstanceId(), "i-123456789012345671");
    }
}

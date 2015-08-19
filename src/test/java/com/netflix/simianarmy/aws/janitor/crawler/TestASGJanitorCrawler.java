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

package com.netflix.simianarmy.aws.janitor.crawler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.SuspendedProcess;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;

public class TestASGJanitorCrawler {

    @Test
    public void testResourceTypes() {
        ASGJanitorCrawler crawler = new ASGJanitorCrawler(createMockAWSClient(createASGList()));
        EnumSet<?> types = crawler.resourceTypes();
        Assert.assertEquals(types.size(), 1);
        Assert.assertEquals(types.iterator().next().name(), "ASG");
    }

    @Test
    public void testInstancesWithNullNames() {
        List<AutoScalingGroup> asgList = createASGList();
        AWSClient awsMock = createMockAWSClient(asgList);
        ASGJanitorCrawler crawler = new ASGJanitorCrawler(awsMock);
        List<Resource> resources = crawler.resources();
        verifyASGList(resources, asgList);
    }

    @Test
    public void testInstancesWithNames() {
        List<AutoScalingGroup> asgList = createASGList();
        String[] asgNames = {"asg1", "asg2"};
        AWSClient awsMock = createMockAWSClient(asgList, asgNames);
        ASGJanitorCrawler crawler = new ASGJanitorCrawler(awsMock);
        List<Resource> resources = crawler.resources(asgNames);
        verifyASGList(resources, asgList);
    }

    @Test
    public void testInstancesWithResourceType() {
        List<AutoScalingGroup> asgList = createASGList();
        AWSClient awsMock = createMockAWSClient(asgList);
        ASGJanitorCrawler crawler = new ASGJanitorCrawler(awsMock);
        for (AWSResourceType resourceType : AWSResourceType.values()) {
            List<Resource> resources = crawler.resources(resourceType);
            if (resourceType == AWSResourceType.ASG) {
                verifyASGList(resources, asgList);
            } else {
                Assert.assertTrue(resources.isEmpty());
            }
        }
    }

    private void verifyASGList(List<Resource> resources, List<AutoScalingGroup> asgList) {
        Assert.assertEquals(resources.size(), asgList.size());
        for (int i = 0; i < resources.size(); i++) {
            AutoScalingGroup asg = asgList.get(i);
            verifyASG(resources.get(i), asg.getAutoScalingGroupName());
        }
    }

    private void verifyASG(Resource asg, String asgName) {
        Assert.assertEquals(asg.getResourceType(), AWSResourceType.ASG);
        Assert.assertEquals(asg.getId(), asgName);
        Assert.assertEquals(asg.getRegion(), "us-east-1");
        Assert.assertEquals(asg.getAdditionalField(ASGJanitorCrawler.ASG_FIELD_SUSPENSION_TIME),
                "2012-12-03T23:00:03");
    }

    private AWSClient createMockAWSClient(List<AutoScalingGroup> asgList, String... asgNames) {
        AWSClient awsMock = mock(AWSClient.class);
        when(awsMock.describeAutoScalingGroups(asgNames)).thenReturn(asgList);
        when(awsMock.region()).thenReturn("us-east-1");
        return awsMock;
    }

    private List<AutoScalingGroup> createASGList() {
        List<AutoScalingGroup> asgList = new LinkedList<AutoScalingGroup>();
        asgList.add(mkASG("asg1"));
        asgList.add(mkASG("asg2"));
        return asgList;
    }

    private AutoScalingGroup mkASG(String asgName) {
        AutoScalingGroup asg = new AutoScalingGroup().withAutoScalingGroupName(asgName);
        // set the suspended processes
        List<SuspendedProcess> sps = new ArrayList<SuspendedProcess>();
        sps.add(new SuspendedProcess().withProcessName("Launch")
                .withSuspensionReason("User suspended at 2012-12-02T23:00:03"));
        sps.add(new SuspendedProcess().withProcessName("AddToLoadBalancer")
                .withSuspensionReason("User suspended at 2012-12-03T23:00:03"));
        asg.setSuspendedProcesses(sps);
        return asg;
    }
}

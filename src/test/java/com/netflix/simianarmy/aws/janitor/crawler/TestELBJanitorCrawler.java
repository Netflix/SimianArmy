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

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;
import org.apache.commons.lang.math.NumberUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestELBJanitorCrawler {

    @Test
    public void testResourceTypes() {
        boolean includeInstances = false;
        AWSClient client = createMockAWSClient();
        addELBsToMock(client, createELBList(includeInstances));
        ELBJanitorCrawler crawler = new ELBJanitorCrawler(client);
        EnumSet<?> types = crawler.resourceTypes();
        Assert.assertEquals(types.size(), 1);
        Assert.assertEquals(types.iterator().next().name(), "ELB");
    }

    @Test
    public void testElbsWithNoInstances() {
        boolean includeInstances = false;
        AWSClient client = createMockAWSClient();
        List<LoadBalancerDescription> elbs = createELBList(includeInstances);
        addELBsToMock(client, elbs);
        ELBJanitorCrawler crawler = new ELBJanitorCrawler(client);
        List<Resource> resources = crawler.resources();
        verifyELBList(resources, elbs);
    }

    @Test
    public void testElbsWithInstances() {
        boolean includeInstances = true;
        AWSClient client = createMockAWSClient();
        List<LoadBalancerDescription> elbs = createELBList(includeInstances);
        addELBsToMock(client, elbs);
        ELBJanitorCrawler crawler = new ELBJanitorCrawler(client);
        List<Resource> resources = crawler.resources();
        verifyELBList(resources, elbs);
    }

    @Test
    public void testElbsWithReferencedASGs() {
        boolean includeInstances = true;
        boolean includeELbs = true;
        AWSClient client = createMockAWSClient();
        List<LoadBalancerDescription> elbs = createELBList(includeInstances);
        List<AutoScalingGroup> asgs = createASGList(includeELbs);
        addELBsToMock(client, elbs);
        addASGsToMock(client, asgs);
        ELBJanitorCrawler crawler = new ELBJanitorCrawler(client);
        List<Resource> resources = crawler.resources();
        verifyELBList(resources, elbs, 1);
    }

    @Test
    public void testElbsWithNoReferencedASGs() {
        boolean includeInstances = true;
        boolean includeELbs = false;
        AWSClient client = createMockAWSClient();
        List<LoadBalancerDescription> elbs = createELBList(includeInstances);
        List<AutoScalingGroup> asgs = createASGList(includeELbs);
        addELBsToMock(client, elbs);
        addASGsToMock(client, asgs);
        ELBJanitorCrawler crawler = new ELBJanitorCrawler(client);
        List<Resource> resources = crawler.resources();
        verifyELBList(resources, elbs, 0);
    }

    @Test
    public void testElbsWithMultipleReferencedASGs() {
        boolean includeInstances = true;
        boolean includeELbs = false;
        AWSClient client = createMockAWSClient();
        List<LoadBalancerDescription> elbs = createELBList(includeInstances);
        List<AutoScalingGroup> asgs = createASGList(includeELbs);
        asgs.get(0).setLoadBalancerNames(Arrays.asList("elb1", "elb2"));
        addELBsToMock(client, elbs);
        addASGsToMock(client, asgs);

        ELBJanitorCrawler crawler = new ELBJanitorCrawler(client);
        List<Resource> resources = crawler.resources();
        verifyELBList(resources, elbs, 1);
    }

    private void verifyELBList(List<Resource> resources, List<LoadBalancerDescription> elbList) {
        verifyELBList(resources, elbList, 0);
    }

    private void verifyELBList(List<Resource> resources, List<LoadBalancerDescription> elbList, int asgCount) {
        Assert.assertEquals(resources.size(), elbList.size());
        for (int i = 0; i < resources.size(); i++) {
            LoadBalancerDescription elb = elbList.get(i);
            verifyELB(resources.get(i), elb, asgCount);
        }
    }

    private void verifyELB(Resource asg, LoadBalancerDescription elb, int asgCount) {
        Assert.assertEquals(asg.getResourceType(), AWSResourceType.ELB);
        Assert.assertEquals(asg.getId(), elb.getLoadBalancerName());
        Assert.assertEquals(asg.getRegion(), "us-east-1");

        int instanceCount = elb.getInstances().size();
        int resourceInstanceCount = NumberUtils.toInt(asg.getAdditionalField("instanceCount"));
        Assert.assertEquals(instanceCount, resourceInstanceCount);

        int resourceASGCount = NumberUtils.toInt(asg.getAdditionalField("referencedASGCount"));
        Assert.assertEquals(resourceASGCount, asgCount);
    }

    private AWSClient createMockAWSClient() {
        AWSClient awsMock = mock(AWSClient.class);
        return awsMock;
    }

    private void addELBsToMock(AWSClient awsMock, List<LoadBalancerDescription> elbList, String... elbNames) {
        when(awsMock.describeElasticLoadBalancers(elbNames)).thenReturn(elbList);
        when(awsMock.region()).thenReturn("us-east-1");
    }

    private void addASGsToMock(AWSClient awsMock, List<AutoScalingGroup> asgList) {
        when(awsMock.describeAutoScalingGroups()).thenReturn(asgList);
        when(awsMock.region()).thenReturn("us-east-1");
    }

    private List<LoadBalancerDescription> createELBList(boolean includeInstances) {
        List<LoadBalancerDescription> elbList = new LinkedList<>();
        elbList.add(mkELB("elb1", includeInstances));
        elbList.add(mkELB("elb2", includeInstances));
        return elbList;
    }

    private LoadBalancerDescription mkELB(String elbName, boolean includeInstances) {
        LoadBalancerDescription elb = new LoadBalancerDescription().withLoadBalancerName(elbName);
        if (includeInstances) {
            List<Instance> instances = new LinkedList<>();
            Instance i1 = new Instance().withInstanceId("i-000001");
            Instance i2 = new Instance().withInstanceId("i-000002");
            elb.setInstances(instances);
        }
        return elb;
    }

    private List<AutoScalingGroup> createASGList(boolean includeElbs) {
        List<AutoScalingGroup> asgList = new LinkedList<AutoScalingGroup>();
        if (includeElbs) {
            asgList.add(mkASG("asg1", "elb1"));
            asgList.add(mkASG("asg2", "elb2"));
        } else {
            asgList.add(mkASG("asg1", null));
            asgList.add(mkASG("asg2", null));
        }
        return asgList;
    }

    private AutoScalingGroup mkASG(String asgName, String elb) {
        AutoScalingGroup asg = new AutoScalingGroup().withAutoScalingGroupName(asgName);
        asg.setLoadBalancerNames(Arrays.asList(elb));
        return asg;
    }

}

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

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.amazonaws.services.autoscaling.model.AutoScalingInstanceDetails;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;

public class TestInstanceJanitorCrawler {

    @Test
    public void testResourceTypes() {
        List<AutoScalingInstanceDetails> instanceDetailsList = createInstanceDetailsList();
        List<Instance> instanceList = createInstanceList();
        InstanceJanitorCrawler crawler = new InstanceJanitorCrawler(createMockAWSClient(
                instanceDetailsList, instanceList));
        EnumSet<?> types = crawler.resourceTypes();
        Assert.assertEquals(types.size(), 1);
        Assert.assertEquals(types.iterator().next().name(), "INSTANCE");
    }

    @Test
    public void testInstancesWithNullIds() {
        List<AutoScalingInstanceDetails> instanceDetailsList = createInstanceDetailsList();
        List<Instance> instanceList = createInstanceList();
        AWSClient awsMock = createMockAWSClient(instanceDetailsList, instanceList);
        InstanceJanitorCrawler crawler = new InstanceJanitorCrawler(awsMock);
        List<Resource> resources = crawler.resources();
        verifyInstanceList(resources, instanceDetailsList);
    }

    @Test
    public void testInstancesWithIds() {
        List<AutoScalingInstanceDetails> instanceDetailsList = createInstanceDetailsList();
        List<Instance> instanceList = createInstanceList();
        String[] ids = {"i-12345678901234560", "i-12345678901234561"};
        AWSClient awsMock = createMockAWSClient(instanceDetailsList, instanceList, ids);
        InstanceJanitorCrawler crawler = new InstanceJanitorCrawler(awsMock);
        List<Resource> resources = crawler.resources(ids);
        verifyInstanceList(resources, instanceDetailsList);
    }

    @Test
    public void testInstancesWithResourceType() {
        List<AutoScalingInstanceDetails> instanceDetailsList = createInstanceDetailsList();
        List<Instance> instanceList = createInstanceList();
        AWSClient awsMock = createMockAWSClient(instanceDetailsList, instanceList);
        InstanceJanitorCrawler crawler = new InstanceJanitorCrawler(awsMock);
        for (AWSResourceType resourceType : AWSResourceType.values()) {
            List<Resource> resources = crawler.resources(resourceType);
            if (resourceType == AWSResourceType.INSTANCE) {
                verifyInstanceList(resources, instanceDetailsList);
            } else {
                Assert.assertTrue(resources.isEmpty());
            }
        }
    }

    @Test
    public void testInstancesNotExistingInASG() {
        List<AutoScalingInstanceDetails> instanceDetailsList = Collections.emptyList();
        List<Instance> instanceList = createInstanceList();
        AWSClient awsMock = createMockAWSClient(instanceDetailsList, instanceList);
        InstanceJanitorCrawler crawler = new InstanceJanitorCrawler(awsMock);
        List<Resource> resources = crawler.resources();
        Assert.assertEquals(resources.size(), instanceList.size());
    }

    private void verifyInstanceList(List<Resource> resources, List<AutoScalingInstanceDetails> instanceList) {
        Assert.assertEquals(resources.size(), instanceList.size());
        for (int i = 0; i < resources.size(); i++) {
            AutoScalingInstanceDetails instance = instanceList.get(i);
            verifyInstance(resources.get(i), instance.getInstanceId(), instance.getAutoScalingGroupName());
        }
    }

    private void verifyInstance(Resource instance, String instanceId, String asgName) {
        Assert.assertEquals(instance.getResourceType(), AWSResourceType.INSTANCE);
        Assert.assertEquals(instance.getId(), instanceId);
        Assert.assertEquals(instance.getRegion(), "us-east-1");
        Assert.assertEquals(instance.getAdditionalField(InstanceJanitorCrawler.INSTANCE_FIELD_ASG_NAME), asgName);
        Assert.assertEquals(((AWSResource) instance).getAWSResourceState(), "running");
    }

    private AWSClient createMockAWSClient(List<AutoScalingInstanceDetails> instanceDetailsList,
            List<Instance> instanceList, String... ids) {
        AWSClient awsMock = mock(AWSClient.class);
        when(awsMock.describeAutoScalingInstances(ids)).thenReturn(instanceDetailsList);
        when(awsMock.describeInstances(ids)).thenReturn(instanceList);
        when(awsMock.region()).thenReturn("us-east-1");
        return awsMock;
    }

    private List<AutoScalingInstanceDetails> createInstanceDetailsList() {
        List<AutoScalingInstanceDetails> instanceList = new LinkedList<AutoScalingInstanceDetails>();
        instanceList.add(mkInstanceDetails("i-12345678901234560", "asg1"));
        instanceList.add(mkInstanceDetails("i-12345678901234561", "asg2"));
        return instanceList;
    }

    private AutoScalingInstanceDetails mkInstanceDetails(String instanceId, String asgName) {
        return new AutoScalingInstanceDetails().withInstanceId(instanceId).withAutoScalingGroupName(asgName);
    }

    private List<Instance> createInstanceList() {
        List<Instance> instanceList = new LinkedList<Instance>();
        instanceList.add(mkInstance("i-12345678901234560"));
        instanceList.add(mkInstance("i-12345678901234561"));
        return instanceList;
    }

    private Instance mkInstance(String instanceId) {
        return new Instance().withInstanceId(instanceId)
                .withState(new InstanceState().withName("running"));
    }

}

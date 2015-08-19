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
// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.aws.conformity.rule;

import com.amazonaws.services.ec2.model.Instance;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.netflix.simianarmy.conformity.AutoScalingGroup;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.Conformity;
import junit.framework.Assert;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.doReturn;


public class TestInstanceInVPC {
    private static final String VPC_INSTANCE_ID = "abc-123";
    private static final String INSTANCE_ID = "zxy-098";
    private static final String REGION = "eu-west-1";

    @Spy
    private InstanceInVPC instanceInVPC = new InstanceInVPC();

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        List<Instance> instanceList = Lists.newArrayList();
        Instance instance = new Instance().withInstanceId(VPC_INSTANCE_ID).withVpcId("12345");
        instanceList.add(instance);
        doReturn(instanceList).when(instanceInVPC).getAWSInstances(REGION, VPC_INSTANCE_ID);
        List<Instance> instanceList2 = Lists.newArrayList();
        Instance instance2 = new Instance().withInstanceId(INSTANCE_ID);
        instanceList2.add(instance2);
        doReturn(instanceList2).when(instanceInVPC).getAWSInstances(REGION, INSTANCE_ID);

    }

    @Test
    public void testCheckSoloInstances() throws Exception {
        Set<String> list = Sets.newHashSet();
        list.add(VPC_INSTANCE_ID);
        list.add(INSTANCE_ID);
        Cluster cluster = new Cluster("SoloInstances", REGION, list);
        Conformity result = instanceInVPC.check(cluster);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getRuleId(), instanceInVPC.getName());
        Assert.assertEquals(result.getFailedComponents().size(), 1);
        Assert.assertEquals(result.getFailedComponents().iterator().next(), INSTANCE_ID);
    }

    @Test
    public void testAsgInstances() throws Exception {
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup("Conforming", VPC_INSTANCE_ID);
        Cluster conformingCluster = new Cluster("Conforming", REGION, autoScalingGroup);
        Conformity result = instanceInVPC.check(conformingCluster);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getRuleId(), instanceInVPC.getName());
        Assert.assertEquals(result.getFailedComponents().size(), 0);

        autoScalingGroup = new AutoScalingGroup("NonConforming", INSTANCE_ID);
        Cluster nonConformingCluster = new Cluster("NonConforming", REGION, autoScalingGroup);
        result = instanceInVPC.check(nonConformingCluster);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getRuleId(), instanceInVPC.getName());
        Assert.assertEquals(result.getFailedComponents().size(), 1);
        Assert.assertEquals(result.getFailedComponents().iterator().next(), INSTANCE_ID);
    }
}

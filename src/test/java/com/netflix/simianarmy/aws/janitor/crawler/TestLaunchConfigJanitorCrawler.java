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
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.google.common.collect.Lists;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;
import org.apache.commons.lang.Validate;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.EnumSet;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestLaunchConfigJanitorCrawler {

    @Test
    public void testResourceTypes() {
        int n = 2;
        String[] lcNames = {"launchConfig1", "launchConfig2"};
        LaunchConfigJanitorCrawler crawler = new LaunchConfigJanitorCrawler(createMockAWSClient(
                createASGList(n), createLaunchConfigList(n), lcNames));
        EnumSet<?> types = crawler.resourceTypes();
        Assert.assertEquals(types.size(), 1);
        Assert.assertEquals(types.iterator().next().name(), "LAUNCH_CONFIG");
    }

    @Test
    public void testInstancesWithNullNames() {
        int n = 2;
        List<LaunchConfiguration> lcList = createLaunchConfigList(n);
        LaunchConfigJanitorCrawler crawler = new LaunchConfigJanitorCrawler(createMockAWSClient(
                createASGList(n), lcList));
        List<Resource> resources = crawler.resources();
        verifyLaunchConfigList(resources, lcList);
    }

    @Test
    public void testInstancesWithNames() {
        int n = 2;
        String[] lcNames = {"launchConfig1", "launchConfig2"};
        List<LaunchConfiguration> lcList = createLaunchConfigList(n);
        LaunchConfigJanitorCrawler crawler = new LaunchConfigJanitorCrawler(createMockAWSClient(
                createASGList(n), lcList, lcNames));
        List<Resource> resources = crawler.resources(lcNames);
        verifyLaunchConfigList(resources, lcList);
    }

    @Test
    public void testInstancesWithResourceType() {
        int n = 2;
        List<LaunchConfiguration> lcList = createLaunchConfigList(n);
        LaunchConfigJanitorCrawler crawler = new LaunchConfigJanitorCrawler(createMockAWSClient(
                createASGList(n), lcList));
        for (AWSResourceType resourceType : AWSResourceType.values()) {
            List<Resource> resources = crawler.resources(resourceType);
            if (resourceType == AWSResourceType.LAUNCH_CONFIG) {
                verifyLaunchConfigList(resources, lcList);
            } else {
                Assert.assertTrue(resources.isEmpty());
            }
        }
    }

    private void verifyLaunchConfigList(List<Resource> resources, List<LaunchConfiguration> lcList) {
        Assert.assertEquals(resources.size(), lcList.size());
        for (int i = 0; i < resources.size(); i++) {
            LaunchConfiguration lc = lcList.get(i);
            if (i % 2 == 0) {
                verifyLaunchConfig(resources.get(i), lc.getLaunchConfigurationName(), true);
            } else {
                verifyLaunchConfig(resources.get(i), lc.getLaunchConfigurationName(), null);
            }
        }
    }

    private void verifyLaunchConfig(Resource launchConfig, String lcName, Boolean usedByASG) {
        Assert.assertEquals(launchConfig.getResourceType(), AWSResourceType.LAUNCH_CONFIG);
        Assert.assertEquals(launchConfig.getId(), lcName);
        Assert.assertEquals(launchConfig.getRegion(), "us-east-1");
        if (usedByASG != null) {
            Assert.assertEquals(launchConfig.getAdditionalField(
                    LaunchConfigJanitorCrawler.LAUNCH_CONFIG_FIELD_USED_BY_ASG), usedByASG.toString());
        }
    }

    private AWSClient createMockAWSClient(List<AutoScalingGroup> asgList,
                                          List<LaunchConfiguration> lcList, String... lcNames) {
        AWSClient awsMock = mock(AWSClient.class);
        when(awsMock.describeAutoScalingGroups()).thenReturn(asgList);
        when(awsMock.describeLaunchConfigurations(lcNames)).thenReturn(lcList);
        when(awsMock.region()).thenReturn("us-east-1");
        return awsMock;
    }

    private List<LaunchConfiguration> createLaunchConfigList(int n) {
        List<LaunchConfiguration> lcList = Lists.newArrayList();
        for (int i = 1; i <= n; i++) {
            lcList.add(mkLaunchConfig("launchConfig" + i));
        }
        return lcList;
    }

    private LaunchConfiguration mkLaunchConfig(String lcName) {
        return new LaunchConfiguration().withLaunchConfigurationName(lcName);
    }

    private List<AutoScalingGroup> createASGList(int n) {
        Validate.isTrue(n > 0);
        List<AutoScalingGroup> asgList = Lists.newArrayList();
        for (int i = 1; i <= n; i += 2) {
            asgList.add(mkASG("asg" + i, "launchConfig" + i));
        }
        return asgList;
    }

    private AutoScalingGroup mkASG(String asgName, String lcName) {
        return new AutoScalingGroup().withAutoScalingGroupName(asgName).withLaunchConfigurationName(lcName);
    }

}

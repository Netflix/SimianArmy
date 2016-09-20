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
package com.netflix.simianarmy.aws.conformity;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.TagDescription;
import com.google.common.collect.Maps;
import com.netflix.simianarmy.aws.conformity.crawler.AWSClusterCrawler;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.conformity.BasicConformityMonkeyContext;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.conformity.Cluster;
import junit.framework.Assert;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestASGOwnerEmailTag {
    
    private static final String ASG1 = "asg1";
    private static final String ASG2 = "asg2";
    private static final String OWNER_TAG_KEY = "owner";
    private static final String OWNER_TAG_VALUE = "tyler@paperstreet.com";
    private static final String REGION = "eu-west-1";

    @Test
    public void testForOwnerTag() {
        Properties properties = new Properties();
        BasicConformityMonkeyContext ctx = new BasicConformityMonkeyContext();

        List<AutoScalingGroup> asgList = createASGList();
        String[] asgNames = {ASG1, ASG2};

        AWSClient awsMock = createMockAWSClient(asgList, asgNames);
        Map<String, AWSClient> regionToAwsClient = Maps.newHashMap();
        regionToAwsClient.put("us-east-1", awsMock);
        AWSClusterCrawler clusterCrawler = new AWSClusterCrawler(regionToAwsClient, new BasicConfiguration(properties));

        List<Cluster> clusters = clusterCrawler.clusters(asgNames);
        
        Assert.assertTrue(OWNER_TAG_VALUE.equalsIgnoreCase(clusters.get(0).getOwnerEmail()));
        Assert.assertNull(clusters.get(1).getOwnerEmail());
    }

    private List<AutoScalingGroup> createASGList() {
        List<AutoScalingGroup> asgList = new LinkedList<AutoScalingGroup>();
        asgList.add(makeASG(ASG1, OWNER_TAG_VALUE));
        asgList.add(makeASG(ASG2, null));
        return asgList;
    }

    private AutoScalingGroup makeASG(String asgName, String ownerEmail) {
        TagDescription tag = new TagDescription().withKey(OWNER_TAG_KEY).withValue(ownerEmail);
        AutoScalingGroup asg = new AutoScalingGroup()
            .withAutoScalingGroupName(asgName)
            .withTags(tag);
        return asg;
    }
    
    private AWSClient createMockAWSClient(List<AutoScalingGroup> asgList, String... asgNames) {
        AWSClient awsMock = mock(AWSClient.class);
        when(awsMock.describeAutoScalingGroups(asgNames)).thenReturn(asgList);
        return awsMock;
    }
}
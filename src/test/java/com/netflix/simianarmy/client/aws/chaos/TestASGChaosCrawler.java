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
package com.netflix.simianarmy.client.aws.chaos;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.TagDescription;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.tunable.TunableInstanceGroup;

public class TestASGChaosCrawler {
    private final ASGChaosCrawler crawler;

    private AutoScalingGroup mkAsg(String asgName, String instanceId) {
        AutoScalingGroup asg = new AutoScalingGroup();
        asg.setAutoScalingGroupName(asgName);
        Instance inst = new Instance();
        inst.setInstanceId(instanceId);
        asg.setInstances(Arrays.asList(inst));
        return asg;
    }

    private final AWSClient awsMock;

    public TestASGChaosCrawler() {
        awsMock = mock(AWSClient.class);
        crawler = new ASGChaosCrawler(awsMock);
    }

    @Test
    public void testGroupTypes() {
        EnumSet<?> types = crawler.groupTypes();
        Assert.assertEquals(types.size(), 1);
        Assert.assertEquals(types.iterator().next().name(), "ASG");
    }

    @Test
    public void testGroups() {
        List<AutoScalingGroup> asgList = new LinkedList<AutoScalingGroup>();
        asgList.add(mkAsg("asg1", "i-123456789012345670"));
        asgList.add(mkAsg("asg2", "i-123456789012345671"));

        when(awsMock.describeAutoScalingGroups((String[]) null)).thenReturn(asgList);

        List<InstanceGroup> groups = crawler.groups();

        verify(awsMock, times(1)).describeAutoScalingGroups((String[]) null);

        Assert.assertEquals(groups.size(), 2);

        Assert.assertEquals(groups.get(0).type(), ASGChaosCrawler.Types.ASG);
        Assert.assertEquals(groups.get(0).name(), "asg1");
        Assert.assertEquals(groups.get(0).instances().size(), 1);
        Assert.assertEquals(groups.get(0).instances().get(0), "i-123456789012345670");

        Assert.assertEquals(groups.get(1).type(), ASGChaosCrawler.Types.ASG);
        Assert.assertEquals(groups.get(1).name(), "asg2");
        Assert.assertEquals(groups.get(1).instances().size(), 1);
        Assert.assertEquals(groups.get(1).instances().get(0), "i-123456789012345671");
    }
    
    @Test
    public void testFindAggressionCoefficient() {
      AutoScalingGroup asg1 = mkAsg("asg1", "i-123456789012345670");
      Set<TagDescription> tagDescriptions = new HashSet<>();
      tagDescriptions.add(makeTunableTag("1.0"));
      asg1.setTags(tagDescriptions);
      
      double aggression = crawler.findAggressionCoefficient(asg1);
      
      Assert.assertEquals(aggression, 1.0);
    }
    
    @Test
    public void testFindAggressionCoefficient_two() {
      AutoScalingGroup asg1 = mkAsg("asg1", "i-123456789012345670");
      Set<TagDescription> tagDescriptions = new HashSet<>();
      tagDescriptions.add(makeTunableTag("2.0"));
      asg1.setTags(tagDescriptions);
      
      double aggression = crawler.findAggressionCoefficient(asg1);
      
      Assert.assertEquals(aggression, 2.0);
    }
    
    @Test
    public void testFindAggressionCoefficient_null() {
      AutoScalingGroup asg1 = mkAsg("asg1", "i-123456789012345670");
      Set<TagDescription> tagDescriptions = new HashSet<>();
      tagDescriptions.add(makeTunableTag(null));
      asg1.setTags(tagDescriptions);
      
      double aggression = crawler.findAggressionCoefficient(asg1);
      
      Assert.assertEquals(aggression, 1.0);
    }

    @Test
    public void testFindAggressionCoefficient_unparsable() {
      AutoScalingGroup asg1 = mkAsg("asg1", "i-123456789012345670");
      Set<TagDescription> tagDescriptions = new HashSet<>();
      tagDescriptions.add(makeTunableTag("not a number"));
      asg1.setTags(tagDescriptions);
      
      double aggression = crawler.findAggressionCoefficient(asg1);
      
      Assert.assertEquals(aggression, 1.0);
    }

    private TagDescription makeTunableTag(String value) {
      TagDescription desc = new TagDescription();
      desc.setKey("chaosMonkey.aggressionCoefficient");
      desc.setValue(value);
      return desc;
    }
    
    @Test 
    public void testGetInstanceGroup_basic() {
      AutoScalingGroup asg = mkAsg("asg1", "i-123456789012345670");

      InstanceGroup group = crawler.getInstanceGroup(asg, 1.0);
      
      Assert.assertTrue( (group instanceof BasicInstanceGroup) );
      Assert.assertFalse( (group instanceof TunableInstanceGroup) );
    }

    @Test 
    public void testGetInstanceGroup_tunable() {
      AutoScalingGroup asg = mkAsg("asg1", "i-123456789012345670");

      InstanceGroup group = crawler.getInstanceGroup(asg, 2.0);
      
      Assert.assertTrue( (group instanceof TunableInstanceGroup) );
    }
}

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
package com.netflix.simianarmy.conformity;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.apache.commons.lang.StringUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.collect.Maps;
import com.netflix.simianarmy.aws.conformity.rule.CrossZoneLoadBalancing;

public class TestCrossZoneLoadBalancing extends CrossZoneLoadBalancing {
    private final Map<String, String> asgToElbs = Maps.newHashMap();
    private final Map<String, Boolean> elbsToCZLB = Maps.newHashMap();
  
    @BeforeClass
    private void init() {
        asgToElbs.put("asg1", "elb1,elb2");
        asgToElbs.put("asg2", "elb1");
        asgToElbs.put("asg3", "");
        elbsToCZLB.put("elb1", true);
    }

    @Test
    public void testDisabledCrossZoneLoadBalancing() {
        Cluster cluster = new Cluster("cluster1", "us-east-1", new AutoScalingGroup("asg1"));
        Conformity result = check(cluster);
        Assert.assertEquals(result.getRuleId(), getName());
        Assert.assertEquals(result.getFailedComponents().size(), 1);
        Assert.assertEquals(result.getFailedComponents().iterator().next(), "elb2");
    }

    @Test
    public void testEnabledCrossZoneLoadBalancing() {
        Cluster cluster = new Cluster("cluster1", "us-east-1", new AutoScalingGroup("asg2"));
        Conformity result = check(cluster);
        Assert.assertEquals(result.getRuleId(), getName());
        Assert.assertEquals(result.getFailedComponents().size(), 0);
    }
    
    @Test
    public void testAsgWithoutElb() {
        Cluster cluster = new Cluster("cluster3", "us-east-1", new AutoScalingGroup("asg3"));
        Conformity result = check(cluster);
        Assert.assertEquals(result.getRuleId(), getName());
        Assert.assertEquals(result.getFailedComponents().size(), 0);
    }
    
    @Override
    protected List<String> getLoadBalancerNamesForAsg(String region, String asgName) {
        return Arrays.asList(StringUtils.split(asgToElbs.get(asgName), ","));
    }

    @Override
    protected boolean isCrossZoneLoadBalancingEnabled(String region, String lbName) {
        Boolean enabled = elbsToCZLB.get(lbName);
        return (enabled == null) ? false : enabled;
    }
    
    
}

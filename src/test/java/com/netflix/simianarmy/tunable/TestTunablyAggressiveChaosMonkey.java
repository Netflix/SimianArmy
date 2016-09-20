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
package com.netflix.simianarmy.tunable;

import com.amazonaws.services.autoscaling.model.TagDescription;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext;

import java.util.Collections;

public class TestTunablyAggressiveChaosMonkey {
  private enum GroupTypes implements GroupType {
    TYPE_A, TYPE_B
  };

  @Test
  public void testFullProbability_basic() {
    TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("fullProbability.properties");

    TunablyAggressiveChaosMonkey chaos = new TunablyAggressiveChaosMonkey(ctx);

    InstanceGroup basic = new BasicInstanceGroup("basic", GroupTypes.TYPE_A, "region", Collections.<TagDescription>emptyList());
    
    double probability = chaos.getEffectiveProbability(basic);
    
    Assert.assertEquals(probability, 1.0);
  }

  @Test
  public void testFullProbability_tuned() {
    TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("fullProbability.properties");

    TunablyAggressiveChaosMonkey chaos = new TunablyAggressiveChaosMonkey(ctx);

    TunableInstanceGroup tuned = new TunableInstanceGroup("basic", GroupTypes.TYPE_A, "region", Collections.<TagDescription>emptyList());
    tuned.setAggressionCoefficient(0.5);
    
    double probability = chaos.getEffectiveProbability(tuned);
    
    Assert.assertEquals(probability, 0.5);
  }
}

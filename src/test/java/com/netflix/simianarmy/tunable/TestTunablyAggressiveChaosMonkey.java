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

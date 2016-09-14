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

import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

/**
 * This class modifies the probability by multiplying the configured
 * probability by the aggression coefficient tag on the instance group.
 * 
 * @author jeffggardner
 */
public class TunablyAggressiveChaosMonkey extends BasicChaosMonkey {

  public TunablyAggressiveChaosMonkey(Context ctx) {
    super(ctx);
  }

  /**
   * Gets the tuned probability value, returns 0 if the group is not
   * enabled. Calls getEffectiveProbability and modifies that value if 
   * the instance group is a TunableInstanceGroup.
   * 
   * @param group The instance group
   * @return the effective probability value for the instance group
   */
  @Override
  protected double getEffectiveProbability(InstanceGroup group) {

    if (!isGroupEnabled(group)) {
      return 0;
    }

    double probability = getEffectiveProbabilityFromCfg(group);
    
    // if this instance group is tunable, then factor in the aggression coefficient
    if (group instanceof TunableInstanceGroup ) {
      TunableInstanceGroup tunable = (TunableInstanceGroup) group;
      probability *= tunable.getAggressionCoefficient();
    }
    
    return probability; 
  }
}

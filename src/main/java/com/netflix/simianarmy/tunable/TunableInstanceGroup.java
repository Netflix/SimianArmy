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
import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;

import java.util.List;

/**
 * Allows for individual InstanceGroups to alter the aggressiveness
 * of ChaosMonkey.
 * 
 * @author jeffggardner
 *
 */
public class TunableInstanceGroup extends BasicInstanceGroup {
  
  public TunableInstanceGroup(String name, GroupType type, String region, List<TagDescription> tags) {
    super(name, type, region, tags);
  }

  private double aggressionCoefficient = 1.0;

  /**
   * @return the aggressionCoefficient
   */
  public final double getAggressionCoefficient() {
    return aggressionCoefficient;
  }

  /**
   * @param aggressionCoefficient the aggressionCoefficient to set
   */
  public final void setAggressionCoefficient(double aggressionCoefficient) {
    this.aggressionCoefficient = aggressionCoefficient;
  }

  
}

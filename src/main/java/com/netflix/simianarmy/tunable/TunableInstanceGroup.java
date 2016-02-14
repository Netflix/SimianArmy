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

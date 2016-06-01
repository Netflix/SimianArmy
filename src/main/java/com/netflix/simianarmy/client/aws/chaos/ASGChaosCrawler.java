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
package com.netflix.simianarmy.client.aws.chaos;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.TagDescription;
import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.basic.chaos.BasicChaosMonkey;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.tunable.TunableInstanceGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class ASGChaosCrawler. This will crawl for all available AutoScalingGroups associated with the AWS account.
 */
public class ASGChaosCrawler implements ChaosCrawler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ASGChaosCrawler.class);

    /**
     * The key of the tag that set the aggression coefficient
     */
    private static final String CHAOS_MONKEY_AGGRESSION_COEFFICIENT_KEY = "chaosMonkey.aggressionCoefficient";

    /**
     * The group types Types.
     */
    public enum Types implements GroupType {

        /** only crawls AutoScalingGroups. */
        ASG;
    }

    /** The aws client. */
    private final AWSClient awsClient;

    /**
     * Instantiates a new basic chaos crawler.
     *
     * @param awsClient
     *            the aws client
     */
    public ASGChaosCrawler(AWSClient awsClient) {
        this.awsClient = awsClient;
    }

    /** {@inheritDoc} */
    @Override
    public EnumSet<?> groupTypes() {
        return EnumSet.allOf(Types.class);
    }

    /** {@inheritDoc} */
    @Override
    public List<InstanceGroup> groups() {
        return groups((String[]) null);
    }

    @Override
    public List<InstanceGroup> groups(String... names) {
        List<InstanceGroup> list = new LinkedList<InstanceGroup>();
        
        for (AutoScalingGroup asg : awsClient.describeAutoScalingGroups(names)) {
          
            InstanceGroup ig = getInstanceGroup(asg, findAggressionCoefficient(asg));
           
            for (Instance inst : asg.getInstances()) {
                ig.addInstance(inst.getInstanceId());
            }
            
            list.add(ig);
        }
        return list;
    }

    /**
     * Returns the desired InstanceGroup.  If there is no set aggression coefficient, then it
     * returns the basic impl, otherwise it returns the tunable impl.
     * @param asg The autoscaling group 
     * @return The appropriate {@link InstanceGroup}
     */
    protected InstanceGroup getInstanceGroup(AutoScalingGroup asg, double aggressionCoefficient) {
      InstanceGroup instanceGroup;

      // if coefficient is 1 then the BasicInstanceGroup is fine, otherwise use Tunable
      if (aggressionCoefficient == 1.0) {
          instanceGroup = new BasicInstanceGroup(asg.getAutoScalingGroupName(), Types.ASG, awsClient.region(), asg.getTags());
      } else {
        TunableInstanceGroup tunable = new TunableInstanceGroup(asg.getAutoScalingGroupName(), Types.ASG, awsClient.region(), asg.getTags());
        tunable.setAggressionCoefficient(aggressionCoefficient);
        
        instanceGroup = tunable;
      }
      
      return instanceGroup;
    }
    
    /**
     * Reads tags on AutoScalingGroup looking for the tag for the aggression coefficient 
     * and determines the coefficient value. The default value is 1 if there no tag or 
     * if the value in the tag is not a parsable number.
     * 
     * @param asg The AutoScalingGroup that might have an aggression coefficient tag
     * @return The set or default aggression coefficient.
     */
    protected double findAggressionCoefficient(AutoScalingGroup asg) {

      List<TagDescription> tagDescriptions = asg.getTags();
      
      double aggression = 1.0;

      for (TagDescription tagDescription : tagDescriptions) {

        if ( CHAOS_MONKEY_AGGRESSION_COEFFICIENT_KEY.equalsIgnoreCase(tagDescription.getKey()) ) {
          String value = tagDescription.getValue();

          // prevent NPE on parseDouble
          if (value == null) {
            break;
          }
          
          try {
            aggression = Double.parseDouble(value);
            LOGGER.info("Aggression coefficient of {} found for ASG {}", value, asg.getAutoScalingGroupName());
          } catch (NumberFormatException e) {
            LOGGER.warn("Unparsable value of {} found in tag {} for ASG {}", value, CHAOS_MONKEY_AGGRESSION_COEFFICIENT_KEY, asg.getAutoScalingGroupName());
            aggression = 1.0;
          }

          // stop looking
          break;
        }
      }

      return aggression;
    }
}

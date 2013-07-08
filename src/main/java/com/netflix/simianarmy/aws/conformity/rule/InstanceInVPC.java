package com.netflix.simianarmy.aws.conformity.rule;

import com.google.common.collect.Lists;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.conformity.AutoScalingGroup;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.Conformity;
import com.netflix.simianarmy.conformity.ConformityRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * The class implements a conformity rule to check an instance is in a virtual private cloud.
 */
public class InstanceInVPC implements ConformityRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceInVPC.class);

    private static final String RULE_NAME = "InstanceInVPC";
    private static final String REASON = "VPC_ID not defined";

    @Override
    public Conformity check(Cluster cluster) {
        AWSClient awsClient = new AWSClient(cluster.getRegion());
        Collection<String> failedComponents = Lists.newArrayList();
        //check all instances
        checkInstancesInVPC(cluster.getSoleInstances(), awsClient, failedComponents);
        //check asg instances ( there will be some overlap)
        for (AutoScalingGroup asg : cluster.getAutoScalingGroups()) {
            if (asg.isSuspended()) {
                continue;
            }
            checkInstancesInVPC(asg.getInstances(), awsClient, failedComponents);
        }
        return new Conformity(getName(), failedComponents);
    }

    private void checkInstancesInVPC(Collection<String> instances, AWSClient awsClient,
                                     Collection<String> failedComponents) {
        for (String instanceID : instances) {
            for (com.amazonaws.services.ec2.model.Instance awsInstance : awsClient.describeInstances(instanceID)) {
                if (awsInstance.getVpcId() == null) {
                    LOGGER.info(String.format("Instance %s is not in a virtual private cloud", instanceID));
                    failedComponents.add(instanceID);
                }
            }
        }
    }

    @Override
    public String getName() {
        return RULE_NAME;
    }

    @Override
    public String getNonconformingReason() {
        return REASON;
    }
}
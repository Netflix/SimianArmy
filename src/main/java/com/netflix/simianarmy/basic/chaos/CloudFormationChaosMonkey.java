package com.netflix.simianarmy.basic.chaos;

import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

public class CloudFormationChaosMonkey extends BasicChaosMonkey {

    public CloudFormationChaosMonkey(Context ctx) {
        super(ctx);
    }

    @Override
    protected boolean isGroupEnabled(InstanceGroup group){
        InstanceGroup noSuffixGroup = noSuffixInstanceGroup(group);
        return super.isGroupEnabled(noSuffixGroup);
    }

    @Override
    protected boolean isMaxTerminationCountExceeded(InstanceGroup group){
        InstanceGroup noSuffixGroup = noSuffixInstanceGroup(group);
        return super.isMaxTerminationCountExceeded(noSuffixGroup);
    }

    @Override
    protected double getEffectiveProbability(InstanceGroup group){
        InstanceGroup noSuffixGroup = noSuffixInstanceGroup(group);
        return super.getEffectiveProbability(noSuffixGroup);
    }

    public InstanceGroup noSuffixInstanceGroup(InstanceGroup group) {
        String newName = group.name().replaceAll("(-)([^-]*$)", "");
        InstanceGroup noSuffixGroup = group.copyAs(newName);
        return noSuffixGroup;
    }
}

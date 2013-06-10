package com.netflix.simianarmy.basic.chaos;

import org.testng.Assert;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;


public class TestCloudFormationChaosMonkey {

    @Test
    public void testIsGroupEnabled(){
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group1 = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_A, "region");
        InstanceGroup group2 = new BasicInstanceGroup("new-group-TestGroup2-XCFNGHFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_A, "region");
        assertTrue(chaos.isGroupEnabled(group1));
        assertFalse(chaos.isGroupEnabled(group2));
    }
    
    @Test
    public void testIsMaxTerminationCountExceeded(){
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group1 = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_A, "region");
        assertFalse(chaos.isMaxTerminationCountExceeded(group1));
    }
    
    @Test
    public void testGetEffectiveProbability(){
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group1 = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_A, "region");
        assertEquals(0.543,chaos.getEffectiveProbability(group1));
    }

    @Test
    public void testNoSuffixInstanceGroup(){
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("disabled.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group = new BasicInstanceGroup("new-group-TestGroup-XCFNFNFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_A, "region");
        InstanceGroup newGroup = chaos.noSuffixInstanceGroup(group);
        assertEquals(newGroup.name(), "new-group-TestGroup");
    }
}

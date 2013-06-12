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
        InstanceGroup group1 = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region");
        InstanceGroup group2 = new BasicInstanceGroup("new-group-TestGroup2-XCFNGHFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region");
        assertTrue(chaos.isGroupEnabled(group1));
        assertFalse(chaos.isGroupEnabled(group2));
    }
    
    @Test
    public void testIsMaxTerminationCountExceeded(){
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group1 = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region");
        assertFalse(chaos.isMaxTerminationCountExceeded(group1));
    }
    
    @Test
    public void testGetEffectiveProbability(){
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group1 = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region");
        assertEquals(1.0,chaos.getEffectiveProbability(group1));
    }

    @Test
    public void testNoSuffixInstanceGroup(){
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("disabled.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group = new BasicInstanceGroup("new-group-TestGroup-XCFNFNFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region");
        InstanceGroup newGroup = chaos.noSuffixInstanceGroup(group);
        assertEquals(newGroup.name(), "new-group-TestGroup");
    }
    
    @Test
    public void testGetLastOptInMilliseconds(){
    	TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group = new BasicInstanceGroup("new-group-TestGroup1-XCFNFNFNF",TestChaosMonkeyContext.CrawlerTypes.TYPE_D, "region");
        assertEquals(chaos.getLastOptInMilliseconds(group),2000);
    }
    
    @Test
    public void testCloudFormationChaosMonkeyIntegration() {
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("cloudformation.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        chaos.start();
        chaos.stop();
        Assert.assertEquals(ctx.selectedOn().size(), 1);
        Assert.assertEquals(ctx.terminated().size(), 1);
    }
}

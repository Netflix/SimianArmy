package com.netflix.simianarmy.basic.chaos;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

import com.netflix.simianarmy.chaos.ChaosMonkey;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;


public class TestCloudFormationChaosMonkey {
    public enum Types{
        TEST
     }

    @Test
    public void testisGroupEnabled(){
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("disabled.properties");
        ChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
    }

    @Test
    public void testnoSuffixInstanceGroup(){
        TestChaosMonkeyContext ctx = new TestChaosMonkeyContext("disabled.properties");
        CloudFormationChaosMonkey chaos = new CloudFormationChaosMonkey(ctx);
        InstanceGroup group = new BasicInstanceGroup("new-group-TestGroup-XCFNFNFNF",Types.TEST, "region");
        InstanceGroup newGroup = chaos.noSuffixInstanceGroup(group);
        assertEquals(newGroup.name(), "new-group-TestGroup");
    }
}

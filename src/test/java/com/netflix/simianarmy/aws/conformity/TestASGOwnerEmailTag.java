// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.aws.conformity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.TagDescription;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.client.aws.AWSClient;

import junit.framework.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.LinkedList;

public class TestASGOwnerEmailTag {
    private static final String ASG1 = "asg1";
    private static final String ASG2 = "asg2";
    private static final String INSTANCE_ID = "i-01234567890";
    private static final String OWNER_TAG_KEY = "owner";
    private static final String OWNER_TAG_VALUE = "tyler@paperstreet.com";
    private static final String REGION = "eu-west-1";

    @Test
    public void testForOwnerTag() {
        List<AutoScalingGroup> asgList = createASGList();
        String[] asgNames = {ASG1, ASG2};
        AWSClient awsMock = createMockAWSClient(asgList, asgNames);
        List<Cluster> list = Lists.newArrayList();
    
        for (AutoScalingGroup asg : asgList) {
            List<String> instances = Lists.newArrayList();
            instances.add(INSTANCE_ID);
            com.netflix.simianarmy.conformity.AutoScalingGroup conformityAsg =
                    new com.netflix.simianarmy.conformity.AutoScalingGroup(
                            asg.getAutoScalingGroupName(),
                            instances.toArray(new String[instances.size()]));            
            Cluster cluster = new Cluster(asg.getAutoScalingGroupName(), REGION, conformityAsg);
            List<TagDescription> tagDescriptions = asg.getTags();
            for (TagDescription tagDescription : tagDescriptions) {
                if ( tagDescription.getKey() != null) {
                    if ( OWNER_TAG_KEY.equalsIgnoreCase(tagDescription.getKey()) ) {
                        String value = tagDescription.getValue();
                        if (value != null) {
                            cluster.setOwnerEmail(value);
                        }
                    }
                }
            }
            list.add(cluster);
        }
        
        Assert.assertNotNull(list.get(0).getOwnerEmail());
        Assert.assertTrue(list.get(0).getOwnerEmail().equalsIgnoreCase(OWNER_TAG_VALUE));
        Assert.assertEquals(list.get(1).getOwnerEmail(), null);
    }

    private List<AutoScalingGroup> createASGList() {
        List<AutoScalingGroup> asgList = new LinkedList<AutoScalingGroup>();
        asgList.add(makeASG(ASG1, OWNER_TAG_VALUE));
        asgList.add(makeASG(ASG2, null));
        return asgList;
    }
    
    private AWSClient createMockAWSClient(List<AutoScalingGroup> asgList, String... asgNames) {
        AWSClient awsMock = mock(AWSClient.class);
        when(awsMock.describeAutoScalingGroups(asgNames)).thenReturn(asgList);
        return awsMock;
    }

    private AutoScalingGroup makeASG(String asgName, String ownerEmail) {
        TagDescription tag = new TagDescription().withKey(OWNER_TAG_KEY).withValue(ownerEmail);
        AutoScalingGroup asg = new AutoScalingGroup().withAutoScalingGroupName(asgName).withTags(tag);
        return asg;
    }

}
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
// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.client.aws.chaos;


import com.amazonaws.services.autoscaling.model.TagDescription;
import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.basic.chaos.BasicInstanceGroup;
import com.netflix.simianarmy.chaos.ChaosCrawler;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


import java.util.*;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;

public class TestFilterASGChaosCrawler {

    private ChaosCrawler crawlerMock;
    private ChaosCrawler crawler;
    private String tagKey, tagValue;

    public enum Types implements GroupType {

        /** only crawls AutoScalingGroups. */
        ASG;
    }

    @BeforeTest
    public void beforeTest() {
        crawlerMock = mock(ChaosCrawler.class);
        tagKey = "key-" + UUID.randomUUID().toString();
        tagValue = "tagValue-" + UUID.randomUUID().toString();
        crawler = new FilteringChaosCrawler(crawlerMock, new TagPredicate(tagKey, tagValue));
    }

    @Test
    public void testFilterGroups() {

        List<TagDescription> tagList = new ArrayList<TagDescription>();
        TagDescription td = new TagDescription();
        td.setKey(tagKey);
        td.setValue(tagValue);
        tagList.add(td);

        List<InstanceGroup> listGroup = new LinkedList<InstanceGroup>();
        listGroup.add(new BasicInstanceGroup("asg1", Types.ASG, "region1", tagList) );
        listGroup.add(new BasicInstanceGroup("asg2", Types.ASG, "region2", Collections.<TagDescription>emptyList()) );
        listGroup.add(new BasicInstanceGroup("asg3", Types.ASG, "region3", tagList) );
        listGroup.add(new BasicInstanceGroup("asg4", Types.ASG, "region4", Collections.<TagDescription>emptyList()) );

        when(crawlerMock.groups()).thenReturn(listGroup);
        List<InstanceGroup> groups = crawlerMock.groups();

        assertEquals(groups.size(), 4);

        groups = crawler.groups();



        assertEquals(groups.size(), 2);

        assertEquals(groups.get(0).name(), "asg1");

        assertEquals(groups.get(1).name(), "asg3");
    }


}

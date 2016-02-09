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
//CHECKSTYLE IGNORE Javadoc

package com.netflix.simianarmy.aws.janitor.crawler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.amazonaws.services.ec2.model.Volume;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;

public class TestEBSVolumeJanitorCrawler {

    @Test
    public void testResourceTypes() {
        Date createTime = new Date();
        List<Volume> volumeList = createVolumeList(createTime);
        EBSVolumeJanitorCrawler crawler = new EBSVolumeJanitorCrawler(createMockAWSClient(volumeList));
        EnumSet<?> types = crawler.resourceTypes();
        Assert.assertEquals(types.size(), 1);
        Assert.assertEquals(types.iterator().next().name(), "EBS_VOLUME");
    }

    @Test
    public void testVolumesWithNullIds() {
        Date createTime = new Date();
        List<Volume> volumeList = createVolumeList(createTime);
        EBSVolumeJanitorCrawler crawler = new EBSVolumeJanitorCrawler(createMockAWSClient(volumeList));
        List<Resource> resources = crawler.resources();
        verifyVolumeList(resources, volumeList, createTime);
    }

    @Test
    public void testVolumesWithIds() {
        Date createTime = new Date();
        List<Volume> volumeList = createVolumeList(createTime);
        String[] ids = {"vol-12345678901234567", "vol-12345678901234567"};
        EBSVolumeJanitorCrawler crawler = new EBSVolumeJanitorCrawler(createMockAWSClient(volumeList, ids));
        List<Resource> resources = crawler.resources(ids);
        verifyVolumeList(resources, volumeList, createTime);
    }

    @Test
    public void testVolumesWithResourceType() {
        Date createTime = new Date();
        List<Volume> volumeList = createVolumeList(createTime);
        EBSVolumeJanitorCrawler crawler = new EBSVolumeJanitorCrawler(createMockAWSClient(volumeList));
        for (AWSResourceType resourceType : AWSResourceType.values()) {
            List<Resource> resources = crawler.resources(resourceType);
            if (resourceType == AWSResourceType.EBS_VOLUME) {
                verifyVolumeList(resources, volumeList, createTime);
            } else {
                Assert.assertTrue(resources.isEmpty());
            }
        }
    }

    private void verifyVolumeList(List<Resource> resources, List<Volume> volumeList, Date createTime) {
        Assert.assertEquals(resources.size(), volumeList.size());
        for (int i = 0; i < resources.size(); i++) {
            Volume volume = volumeList.get(i);
            verifyVolume(resources.get(i), volume.getVolumeId(), createTime);
        }
    }

    private void verifyVolume(Resource volume, String volumeId, Date createTime) {
        Assert.assertEquals(volume.getResourceType(), AWSResourceType.EBS_VOLUME);
        Assert.assertEquals(volume.getId(), volumeId);
        Assert.assertEquals(volume.getRegion(), "us-east-1");
        Assert.assertEquals(((AWSResource) volume).getAWSResourceState(), "available");
        Assert.assertEquals(volume.getLaunchTime(), createTime);
    }

    private AWSClient createMockAWSClient(List<Volume> volumeList, String... ids) {
        AWSClient awsMock = mock(AWSClient.class);
        when(awsMock.describeVolumes(ids)).thenReturn(volumeList);
        when(awsMock.region()).thenReturn("us-east-1");
        return awsMock;
    }

    private List<Volume> createVolumeList(Date createTime) {
        List<Volume> volumeList = new LinkedList<Volume>();
        volumeList.add(mkVolume("vol-12345678901234567", createTime));
        volumeList.add(mkVolume("vol-12345678901234567", createTime));
        return volumeList;
    }

    private Volume mkVolume(String volumeId, Date createTime) {
        return new Volume().withVolumeId(volumeId).withState("available").withCreateTime(createTime);
    }

}

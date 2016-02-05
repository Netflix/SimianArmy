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

import com.amazonaws.services.ec2.model.Snapshot;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;

public class TestEBSSnapshotJanitorCrawler {

    @Test
    public void testResourceTypes() {
        Date startTime = new Date();
        List<Snapshot> snapshotList = createSnapshotList(startTime);
        EBSSnapshotJanitorCrawler crawler = new EBSSnapshotJanitorCrawler(createMockAWSClient(snapshotList));
        EnumSet<?> types = crawler.resourceTypes();
        Assert.assertEquals(types.size(), 1);
        Assert.assertEquals(types.iterator().next().name(), "EBS_SNAPSHOT");
    }

    @Test
    public void testSnapshotsWithNullIds() {
        Date startTime = new Date();
        List<Snapshot> snapshotList = createSnapshotList(startTime);
        EBSSnapshotJanitorCrawler crawler = new EBSSnapshotJanitorCrawler(createMockAWSClient(snapshotList));
        List<Resource> resources = crawler.resources();
        verifySnapshotList(resources, snapshotList, startTime);
    }

    @Test
    public void testSnapshotsWithIds() {
        Date startTime = new Date();
        List<Snapshot> snapshotList = createSnapshotList(startTime);
        String[] ids = {"snap-12345678901234567", "snap-12345678901234567"};
        EBSSnapshotJanitorCrawler crawler = new EBSSnapshotJanitorCrawler(createMockAWSClient(snapshotList, ids));
        List<Resource> resources = crawler.resources(ids);
        verifySnapshotList(resources, snapshotList, startTime);
    }

    @Test
    public void testSnapshotsWithResourceType() {
        Date startTime = new Date();
        List<Snapshot> snapshotList = createSnapshotList(startTime);
        EBSSnapshotJanitorCrawler crawler = new EBSSnapshotJanitorCrawler(createMockAWSClient(snapshotList));
        for (AWSResourceType resourceType : AWSResourceType.values()) {
            List<Resource> resources = crawler.resources(resourceType);
            if (resourceType == AWSResourceType.EBS_SNAPSHOT) {
                verifySnapshotList(resources, snapshotList, startTime);
            } else {
                Assert.assertTrue(resources.isEmpty());
            }
        }
    }

    private void verifySnapshotList(List<Resource> resources, List<Snapshot> snapshotList, Date startTime) {
        Assert.assertEquals(resources.size(), snapshotList.size());
        for (int i = 0; i < resources.size(); i++) {
            Snapshot snapshot = snapshotList.get(i);
            verifySnapshot(resources.get(i), snapshot.getSnapshotId(), startTime);
        }
    }

    private void verifySnapshot(Resource snapshot, String snapshotId, Date startTime) {
        Assert.assertEquals(snapshot.getResourceType(), AWSResourceType.EBS_SNAPSHOT);
        Assert.assertEquals(snapshot.getId(), snapshotId);
        Assert.assertEquals(snapshot.getRegion(), "us-east-1");
        Assert.assertEquals(((AWSResource) snapshot).getAWSResourceState(), "completed");
        Assert.assertEquals(snapshot.getLaunchTime(), startTime);
    }

    private AWSClient createMockAWSClient(List<Snapshot> snapshotList, String... ids) {
        AWSClient awsMock = mock(AWSClient.class);
        when(awsMock.describeSnapshots(ids)).thenReturn(snapshotList);
        when(awsMock.region()).thenReturn("us-east-1");
        return awsMock;
    }

    private List<Snapshot> createSnapshotList(Date startTime) {
        List<Snapshot> snapshotList = new LinkedList<Snapshot>();
        snapshotList.add(mkSnapshot("snap-12345678901234567", startTime));
        snapshotList.add(mkSnapshot("snap-12345678901234567", startTime));
        return snapshotList;
    }

    private Snapshot mkSnapshot(String snapshotId, Date startTime) {
        return new Snapshot().withSnapshotId(snapshotId).withState("completed").withStartTime(startTime);
    }

}

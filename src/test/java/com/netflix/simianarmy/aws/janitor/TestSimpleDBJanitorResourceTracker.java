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
// CHECKSTYLE IGNORE MagicNumberCheck
// CHECKSTYLE IGNORE ParameterNumber
package com.netflix.simianarmy.aws.janitor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.mockito.ArgumentCaptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;

public class TestSimpleDBJanitorResourceTracker extends SimpleDBJanitorResourceTracker {

    private static AWSClient makeMockAWSClient() {
        AmazonSimpleDB sdbMock = mock(AmazonSimpleDB.class);
        AWSClient awsClient = mock(AWSClient.class);
        when(awsClient.sdbClient()).thenReturn(sdbMock);
        return awsClient;
    }

    public TestSimpleDBJanitorResourceTracker() {
        super(makeMockAWSClient(), "DOMAIN");
        sdbMock = super.getSimpleDBClient();
    }

    private final AmazonSimpleDB sdbMock;

    @Test
    public void testAddResource() {
        String id = "i-12345678901234567";
        AWSResourceType resourceType = AWSResourceType.INSTANCE;
        Resource.CleanupState state = Resource.CleanupState.MARKED;
        String description = "This is a test resource.";
        String ownerEmail = "owner@test.com";
        String region = "us-east-1";
        String terminationReason = "This is a test termination reason.";
        DateTime now = DateTime.now();
        Date expectedTerminationTime = new Date(now.plusDays(10).getMillis());
        Date markTime = new Date(now.getMillis());
        String fieldName = "fieldName123";
        String fieldValue = "fieldValue456";
        Resource resource = new AWSResource().withId(id).withResourceType(resourceType)
                .withDescription(description).withOwnerEmail(ownerEmail).withRegion(region)
                .withState(state).withTerminationReason(terminationReason)
                .withExpectedTerminationTime(expectedTerminationTime)
                .withMarkTime(markTime).withOptOutOfJanitor(false)
                .setAdditionalField(fieldName, fieldValue);
        ArgumentCaptor<PutAttributesRequest> arg = ArgumentCaptor.forClass(PutAttributesRequest.class);

        TestSimpleDBJanitorResourceTracker tracker = new TestSimpleDBJanitorResourceTracker();

        tracker.addOrUpdate(resource);
        verify(tracker.sdbMock).putAttributes(arg.capture());
        PutAttributesRequest req = arg.getValue();

        Assert.assertEquals(req.getDomainName(), "DOMAIN");
        Assert.assertEquals(req.getItemName(), getSimpleDBItemName(resource));
        Map<String, String> map = new HashMap<String, String>();
        for (ReplaceableAttribute attr : req.getAttributes()) {
            map.put(attr.getName(), attr.getValue());
        }

        Assert.assertEquals(map.remove(AWSResource.FIELD_RESOURCE_ID), id);
        Assert.assertEquals(map.remove(AWSResource.FIELD_DESCRIPTION), description);
        Assert.assertEquals(map.remove(AWSResource.FIELD_EXPECTED_TERMINATION_TIME),
                AWSResource.DATE_FORMATTER.print(expectedTerminationTime.getTime()));
        Assert.assertEquals(map.remove(AWSResource.FIELD_MARK_TIME),
                AWSResource.DATE_FORMATTER.print(markTime.getTime()));
        Assert.assertEquals(map.remove(AWSResource.FIELD_REGION), region);
        Assert.assertEquals(map.remove(AWSResource.FIELD_OWNER_EMAIL), ownerEmail);
        Assert.assertEquals(map.remove(AWSResource.FIELD_RESOURCE_TYPE), resourceType.name());
        Assert.assertEquals(map.remove(AWSResource.FIELD_STATE), state.name());
        Assert.assertEquals(map.remove(AWSResource.FIELD_TERMINATION_REASON), terminationReason);
        Assert.assertEquals(map.remove(AWSResource.FIELD_OPT_OUT_OF_JANITOR), "false");
        Assert.assertEquals(map.remove(fieldName), fieldValue);
        Assert.assertEquals(map.size(), 0);
    }


    @Test
    public void testGetResources() {
        String id1 = "id-1";
        String id2 = "id-2";
        AWSResourceType resourceType = AWSResourceType.INSTANCE;
        Resource.CleanupState state = Resource.CleanupState.MARKED;
        String description = "This is a test resource.";
        String ownerEmail = "owner@test.com";
        String region = "us-east-1";
        String terminationReason = "This is a test termination reason.";
        DateTime now = DateTime.now();
        Date expectedTerminationTime = new Date(now.plusDays(10).getMillis());
        Date markTime = new Date(now.getMillis());
        String fieldName = "fieldName123";
        String fieldValue = "fieldValue456";

        SelectResult result1 = mkSelectResult(id1, resourceType, state, description, ownerEmail,
                region, terminationReason, expectedTerminationTime, markTime, false, fieldName, fieldValue);
        result1.setNextToken("nextToken");
        SelectResult result2 = mkSelectResult(id2, resourceType, state, description, ownerEmail,
                region, terminationReason, expectedTerminationTime, markTime, true, fieldName, fieldValue);

        ArgumentCaptor<SelectRequest> arg = ArgumentCaptor.forClass(SelectRequest.class);

        TestSimpleDBJanitorResourceTracker tracker = new TestSimpleDBJanitorResourceTracker();

        when(tracker.sdbMock.select(any(SelectRequest.class))).thenReturn(result1).thenReturn(result2);

        verifyResources(tracker.getResources(resourceType, state, region),
                id1, id2, resourceType, state, description, ownerEmail,
                region, terminationReason, expectedTerminationTime, markTime, fieldName, fieldValue);

        verify(tracker.sdbMock, times(2)).select(arg.capture());
    }

    private void verifyResources(List<Resource> resources, String id1, String id2, AWSResourceType resourceType,
            Resource.CleanupState state, String description, String ownerEmail, String region,
            String terminationReason, Date expectedTerminationTime, Date markTime, String fieldName,
            String fieldValue) {
        Assert.assertEquals(resources.size(), 2);

        Assert.assertEquals(resources.get(0).getId(), id1);
        Assert.assertEquals(resources.get(0).getResourceType(), resourceType);
        Assert.assertEquals(resources.get(0).getState(), state);
        Assert.assertEquals(resources.get(0).getDescription(), description);
        Assert.assertEquals(resources.get(0).getOwnerEmail(), ownerEmail);
        Assert.assertEquals(resources.get(0).getRegion(), region);
        Assert.assertEquals(resources.get(0).getTerminationReason(), terminationReason);
        Assert.assertEquals(
                AWSResource.DATE_FORMATTER.print(resources.get(0).getExpectedTerminationTime().getTime()),
                AWSResource.DATE_FORMATTER.print(expectedTerminationTime.getTime()));
        Assert.assertEquals(
                AWSResource.DATE_FORMATTER.print(resources.get(0).getMarkTime().getTime()),
                AWSResource.DATE_FORMATTER.print(markTime.getTime()));
        Assert.assertEquals(resources.get(0).getAdditionalField(fieldName), fieldValue);
        Assert.assertEquals(resources.get(0).isOptOutOfJanitor(), false);

        Assert.assertEquals(resources.get(1).getId(), id2);
        Assert.assertEquals(resources.get(1).getResourceType(), resourceType);
        Assert.assertEquals(resources.get(1).getState(), state);
        Assert.assertEquals(resources.get(1).getDescription(), description);
        Assert.assertEquals(resources.get(1).getOwnerEmail(), ownerEmail);
        Assert.assertEquals(resources.get(1).getRegion(), region);
        Assert.assertEquals(resources.get(1).getTerminationReason(), terminationReason);
        Assert.assertEquals(
                AWSResource.DATE_FORMATTER.print(resources.get(1).getExpectedTerminationTime().getTime()),
                AWSResource.DATE_FORMATTER.print(expectedTerminationTime.getTime()));
        Assert.assertEquals(
                AWSResource.DATE_FORMATTER.print(resources.get(1).getMarkTime().getTime()),
                AWSResource.DATE_FORMATTER.print(markTime.getTime()));
        Assert.assertEquals(resources.get(1).isOptOutOfJanitor(), true);
        Assert.assertEquals(resources.get(1).getAdditionalField(fieldName), fieldValue);
    }

    private SelectResult mkSelectResult(String id, AWSResourceType resourceType, Resource.CleanupState state,
            String description, String ownerEmail, String region, String terminationReason,
            Date expectedTerminationTime, Date markTime, boolean optOut, String fieldName, String fieldValue) {
        Item item = new Item();
        List<Attribute> attrs = new LinkedList<Attribute>();
        attrs.add(new Attribute(AWSResource.FIELD_RESOURCE_ID, id));
        attrs.add(new Attribute(AWSResource.FIELD_RESOURCE_TYPE, resourceType.name()));
        attrs.add(new Attribute(AWSResource.FIELD_DESCRIPTION, description));
        attrs.add(new Attribute(AWSResource.FIELD_REGION, region));
        attrs.add(new Attribute(AWSResource.FIELD_STATE, state.name()));
        attrs.add(new Attribute(AWSResource.FIELD_OWNER_EMAIL, ownerEmail));
        attrs.add(new Attribute(AWSResource.FIELD_TERMINATION_REASON, terminationReason));
        attrs.add(new Attribute(AWSResource.FIELD_EXPECTED_TERMINATION_TIME,
                AWSResource.DATE_FORMATTER.print(expectedTerminationTime.getTime())));
        attrs.add(new Attribute(AWSResource.FIELD_MARK_TIME,
                AWSResource.DATE_FORMATTER.print(markTime.getTime())));
        attrs.add(new Attribute(AWSResource.FIELD_OPT_OUT_OF_JANITOR, String.valueOf(optOut)));
        attrs.add(new Attribute(fieldName, fieldValue));

        item.setAttributes(attrs);
        item.setName(String.format("%s-%s-%s", resourceType.name(), id, region));
        SelectResult result = new SelectResult();
        result.setItems(Arrays.asList(item));
        return result;
    }
}

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

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import org.joda.time.DateTime;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestRDSJanitorResourceTracker extends RDSJanitorResourceTracker {
	
    public TestRDSJanitorResourceTracker() {
        super(mock(JdbcTemplate.class), "janitortable");
    }

    @Test
    public void testInit() {        
    	TestRDSJanitorResourceTracker recorder = new TestRDSJanitorResourceTracker();	
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(recorder.getJdbcTemplate()).execute(sqlCap.capture());
        recorder.init();        
        Assert.assertEquals(sqlCap.getValue(), "create table if not exists janitortable ( resourceId varchar(255),  resourceType varchar(255),  region varchar(25),  ownerEmail varchar(255),  description varchar(255),  state varchar(25),  terminationReason varchar(255),  expectedTerminationTime BIGINT,  actualTerminationTime BIGINT,  notificationTime BIGINT,  launchTime BIGINT,  markTime BIGINT,  optOutOfJanitor varchar(8),  additionalFields varchar(4096) )");
    }    

    @SuppressWarnings("unchecked")
    @Test
    public void testInsertNewResource() {
    	// mock the select query that is issued to see if the record already exists
        ArrayList<AWSResource> resources = new ArrayList<>();
        TestRDSJanitorResourceTracker tracker = new TestRDSJanitorResourceTracker();
		when(tracker.getJdbcTemplate().query(Matchers.anyString(), 
         		                            Matchers.any(Object[].class), 
         		                            Matchers.any(RowMapper.class))).thenReturn(resources);
    	    	
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

        ArgumentCaptor<Object> objCap = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        when(tracker.getJdbcTemplate().update(sqlCap.capture(), 
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),
                                              objCap.capture(),
        		                              objCap.capture(),
                                              objCap.capture(),
        		                              objCap.capture())).thenReturn(1);
        tracker.addOrUpdate(resource);

        List<Object> args = objCap.getAllValues();

        Assert.assertEquals(sqlCap.getValue(), "insert into janitortable (resourceId,resourceType,region,ownerEmail,description,state,terminationReason,expectedTerminationTime,actualTerminationTime,notificationTime,launchTime,markTime,optOutOfJanitor,additionalFields) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
        Assert.assertEquals(args.size(), 14);
        Assert.assertEquals(args.get(0).toString(), id);
        Assert.assertEquals(args.get(1).toString(), AWSResourceType.INSTANCE.toString());
        Assert.assertEquals(args.get(2).toString(), region);
        Assert.assertEquals(args.get(3).toString(), ownerEmail);
        Assert.assertEquals(args.get(4).toString(), description);
        Assert.assertEquals(args.get(5).toString(), state.toString());
        Assert.assertEquals(args.get(6).toString(), terminationReason);
        Assert.assertEquals(args.get(7).toString(), expectedTerminationTime.getTime() + "");
        Assert.assertEquals(args.get(8).toString(), "0");
        Assert.assertEquals(args.get(9).toString(), "0");
        Assert.assertEquals(args.get(10).toString(), "0");
        Assert.assertEquals(args.get(11).toString(), markTime.getTime() + "");
        Assert.assertEquals(args.get(12).toString(), "false");
        Assert.assertEquals(args.get(13).toString(), "{\"fieldName123\":\"fieldValue456\"}");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateResource() {
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
        
        // mock the select query that is issued to see if the record already exists
        ArrayList<Resource> resources = new ArrayList<>();
        resources.add(resource);
        TestRDSJanitorResourceTracker tracker = new TestRDSJanitorResourceTracker();
		when(tracker.getJdbcTemplate().query(Matchers.anyString(), 
         		                            Matchers.any(Object[].class), 
         		                            Matchers.any(RowMapper.class))).thenReturn(resources);

		// update the ownerEmail
		ownerEmail = "owner2@test.com";
        Resource newResource = new AWSResource().withId(id).withResourceType(resourceType)
                .withDescription(description).withOwnerEmail(ownerEmail).withRegion(region)
                .withState(state).withTerminationReason(terminationReason)
                .withExpectedTerminationTime(expectedTerminationTime)
                .withMarkTime(markTime).withOptOutOfJanitor(false)
                .setAdditionalField(fieldName, fieldValue);
		
        ArgumentCaptor<Object> objCap = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        when(tracker.getJdbcTemplate().update(sqlCap.capture(), 
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),  
        		                              objCap.capture(),
                                              objCap.capture(),
                                              objCap.capture(),
        		                              objCap.capture(),  
        		                              objCap.capture(),
                                              objCap.capture(),
                                              objCap.capture(),
                                              objCap.capture())).thenReturn(1);
        tracker.addOrUpdate(newResource);

        List<Object> args = objCap.getAllValues();        
        Assert.assertEquals(sqlCap.getValue(), "update janitortable set resourceType=?,region=?,ownerEmail=?,description=?,state=?,terminationReason=?,expectedTerminationTime=?,actualTerminationTime=?,notificationTime=?,launchTime=?,markTime=?,optOutOfJanitor=?,additionalFields=? where resourceId=? and region=?");
        Assert.assertEquals(args.size(), 15);
        Assert.assertEquals(args.get(0).toString(), AWSResourceType.INSTANCE.toString());
        Assert.assertEquals(args.get(1).toString(), region);
        Assert.assertEquals(args.get(2).toString(), ownerEmail);
        Assert.assertEquals(args.get(3).toString(), description);
        Assert.assertEquals(args.get(4).toString(), state.toString());
        Assert.assertEquals(args.get(5).toString(), terminationReason);
        Assert.assertEquals(args.get(6).toString(), expectedTerminationTime.getTime() + "");
        Assert.assertEquals(args.get(7).toString(), "0");
        Assert.assertEquals(args.get(8).toString(), "0");
        Assert.assertEquals(args.get(9).toString(), "0");
        Assert.assertEquals(args.get(10).toString(), markTime.getTime() + "");
        Assert.assertEquals(args.get(11).toString(), "false");
        Assert.assertEquals(args.get(12).toString(), "{\"fieldName123\":\"fieldValue456\"}");
        Assert.assertEquals(args.get(13).toString(), id);
        Assert.assertEquals(args.get(14).toString(), region);
    }

    
    @SuppressWarnings("unchecked")
	@Test
    public void testGetResource() {
        String id1 = "id-1";
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
                
        AWSResource result1 = mkResource(id1, resourceType, state, description, ownerEmail,
                region, terminationReason, expectedTerminationTime, markTime, false, fieldName, fieldValue);

        ArrayList<AWSResource> resources = new ArrayList<>();
        resources.add(result1);
        TestRDSJanitorResourceTracker tracker = new TestRDSJanitorResourceTracker();
		when(tracker.getJdbcTemplate().query(Matchers.anyString(), 
         		                            Matchers.any(Object[].class), 
         		                            Matchers.any(RowMapper.class))).thenReturn(resources);

		Resource resource = tracker.getResource(id1);
        ArrayList<Resource> returnResources = new ArrayList<>();
        returnResources.add(resource);

        verifyResources(returnResources,
                id1, null, resourceType, state, description, ownerEmail,
                region, terminationReason, expectedTerminationTime, markTime, fieldName, fieldValue);
    }

	@SuppressWarnings("unchecked")
	@Test
    public void testGetResourceNotFound() {
        ArrayList<AWSResource> resources = new ArrayList<>();
        TestRDSJanitorResourceTracker tracker = new TestRDSJanitorResourceTracker();
		when(tracker.getJdbcTemplate().query(Matchers.anyString(), 
         		                            Matchers.any(Object[].class), 
         		                            Matchers.any(RowMapper.class))).thenReturn(resources);
		Resource resource = tracker.getResource("id-2");
		Assert.assertNull(resource);
    }

	@SuppressWarnings("unchecked")
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
                
        AWSResource result1 = mkResource(id1, resourceType, state, description, ownerEmail,
                region, terminationReason, expectedTerminationTime, markTime, false, fieldName, fieldValue);
        AWSResource result2 = mkResource(id2, resourceType, state, description, ownerEmail,
                region, terminationReason, expectedTerminationTime, markTime, true, fieldName, fieldValue);

        ArrayList<AWSResource> resources = new ArrayList<>();
        resources.add(result1);
        resources.add(result2);
        TestRDSJanitorResourceTracker tracker = new TestRDSJanitorResourceTracker();
		when(tracker.getJdbcTemplate().query(Matchers.anyString(), 
         		                            Matchers.any(Object[].class), 
         		                            Matchers.any(RowMapper.class))).thenReturn(resources);

        verifyResources(tracker.getResources(resourceType, state, region),
                id1, id2, resourceType, state, description, ownerEmail,
                region, terminationReason, expectedTerminationTime, markTime, fieldName, fieldValue);

    }

    private void verifyResources(List<Resource> resources, String id1, String id2, AWSResourceType resourceType,
            Resource.CleanupState state, String description, String ownerEmail, String region,
            String terminationReason, Date expectedTerminationTime, Date markTime, String fieldName,
            String fieldValue) {
    	
    	if (id2 == null) {
    		Assert.assertEquals(resources.size(), 1);    		
    	} else {
    		Assert.assertEquals(resources.size(), 2);
    	}

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

        if (id2 != null) {
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
    }

    private AWSResource mkResource(String id, AWSResourceType resourceType, Resource.CleanupState state,
            String description, String ownerEmail, String region, String terminationReason,
            Date expectedTerminationTime, Date markTime, boolean optOut, String fieldName, String fieldValue) {

    	Map<String, String> attrs = new HashMap<>();
        attrs.put(AWSResource.FIELD_RESOURCE_ID, id);
        attrs.put(AWSResource.FIELD_RESOURCE_TYPE, resourceType.name());
        attrs.put(AWSResource.FIELD_DESCRIPTION, description);
        attrs.put(AWSResource.FIELD_REGION, region);
        attrs.put(AWSResource.FIELD_STATE, state.name());
        attrs.put(AWSResource.FIELD_OWNER_EMAIL, ownerEmail);
        attrs.put(AWSResource.FIELD_TERMINATION_REASON, terminationReason);
        attrs.put(AWSResource.FIELD_EXPECTED_TERMINATION_TIME,
                AWSResource.DATE_FORMATTER.print(expectedTerminationTime.getTime()));
        attrs.put(AWSResource.FIELD_MARK_TIME,
                AWSResource.DATE_FORMATTER.print(markTime.getTime()));
        attrs.put(AWSResource.FIELD_OPT_OUT_OF_JANITOR, String.valueOf(optOut));
        attrs.put(fieldName, fieldValue);

        return AWSResource.parseFieldtoValueMap(attrs);
    }
}

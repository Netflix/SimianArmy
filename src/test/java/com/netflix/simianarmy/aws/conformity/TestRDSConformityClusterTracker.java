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
package com.netflix.simianarmy.aws.conformity;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.Conformity;

public class TestRDSConformityClusterTracker extends RDSConformityClusterTracker {
    public TestRDSConformityClusterTracker() {
        super(mock(JdbcTemplate.class), "conformitytable");
    }

    @Test
    public void testInit() {        
    	TestRDSConformityClusterTracker recorder = new TestRDSConformityClusterTracker();	
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        Mockito.doNothing().when(recorder.getJdbcTemplate()).execute(sqlCap.capture());
        recorder.init();        
        Assert.assertEquals(sqlCap.getValue(), "create table if not exists conformitytable ( cluster varchar(255), region varchar(25), ownerEmail varchar(255), isConforming varchar(10), isOptedOut varchar(10), updateTimestamp BIGINT, excludedRules varchar(4096), conformities varchar(4096), conformityRules varchar(4096) )");
    }    

    @SuppressWarnings("unchecked")
    @Test
    public void testInsertNewCluster() {
    	// mock the select query that is issued to see if the record already exists
        ArrayList<Cluster> clusters = new ArrayList<>();
        TestRDSConformityClusterTracker tracker = new TestRDSConformityClusterTracker();
		when(tracker.getJdbcTemplate().query(Matchers.anyString(), 
         		                            Matchers.any(Object[].class), 
         		                            Matchers.any(RowMapper.class))).thenReturn(clusters);
    	    	
        Cluster cluster1 = new Cluster("clustername1", "us-west-1");
        cluster1.setUpdateTime(new Date());
        ArrayList<String> list = new ArrayList<>();
        list.add("one");
        list.add("two");
        cluster1.updateConformity(new Conformity("rule1",list));
        list.add("three");
        cluster1.updateConformity(new Conformity("rule2",list));

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
        		                              objCap.capture())).thenReturn(1);
        tracker.addOrUpdate(cluster1);

        List<Object> args = objCap.getAllValues();        
        Assert.assertEquals(args.size(), 9);
        Assert.assertEquals(args.get(0).toString(), "clustername1");
        Assert.assertEquals(args.get(1).toString(), "us-west-1");
        Assert.assertEquals(args.get(7).toString(), "{\"rule1\":\"one,two\",\"rule2\":\"one,two,three\"}");
        Assert.assertEquals(args.get(8).toString(), "rule1,rule2");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateCluster() {
        Cluster cluster1 = new Cluster("clustername1", "us-west-1");
        Date date = new Date();
        cluster1.setUpdateTime(date);

        // mock the select query that is issued to see if the record already exists
        ArrayList<Cluster> clusters = new ArrayList<>();
        clusters.add(cluster1);
        TestRDSConformityClusterTracker tracker = new TestRDSConformityClusterTracker();
		when(tracker.getJdbcTemplate().query(Matchers.anyString(), 
         		                            Matchers.any(Object[].class), 
         		                            Matchers.any(RowMapper.class))).thenReturn(clusters);
    	    	
		cluster1.setOwnerEmail("newemail@test.com");
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
        		                              objCap.capture())).thenReturn(1);
        tracker.addOrUpdate(cluster1);

        List<Object> args = objCap.getAllValues();        
        Assert.assertEquals(sqlCap.getValue(), "update conformitytable set ownerEmail=?,isConforming=?,isOptedOut=?,updateTimestamp=?,excludedRules=?,conformities=?,conformityRules=? where cluster=? and region=?");
        Assert.assertEquals(args.size(), 9);
        Assert.assertEquals(args.get(0).toString(), "newemail@test.com");
        Assert.assertEquals(args.get(1).toString(), "false");
        Assert.assertEquals(args.get(2).toString(), "false");
        Assert.assertEquals(args.get(3).toString(), date.getTime() + "");
        Assert.assertEquals(args.get(7).toString(), "clustername1");
        Assert.assertEquals(args.get(8).toString(), "us-west-1");
    }
    
    @SuppressWarnings("unchecked")
	@Test
    public void testGetCluster() {
        Cluster cluster1 = new Cluster("clustername1", "us-west-1");
       
        ArrayList<Cluster> clusters = new ArrayList<>();
        clusters.add(cluster1);
        TestRDSConformityClusterTracker tracker = new TestRDSConformityClusterTracker();
        
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        
		when(tracker.getJdbcTemplate().query(sqlCap.capture(), 
											 Matchers.any(Object[].class), 
         		                             Matchers.any(RowMapper.class))).thenReturn(clusters);
		Cluster result = tracker.getCluster("clustername1", "us-west-1");
		Assert.assertNotNull(result);
		Assert.assertEquals(sqlCap.getValue(), "select * from conformitytable where cluster = ? and region = ?");
    }

    @SuppressWarnings("unchecked")
	@Test
    public void testGetClusters() {
        Cluster cluster1 = new Cluster("clustername1", "us-west-1");
        Cluster cluster2 = new Cluster("clustername1", "us-west-2");
        Cluster cluster3 = new Cluster("clustername1", "us-east-1");
       
        ArrayList<Cluster> clusters = new ArrayList<>();
        clusters.add(cluster1);
        clusters.add(cluster2);
        clusters.add(cluster3);
        TestRDSConformityClusterTracker tracker = new TestRDSConformityClusterTracker();
        
        ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
        
		when(tracker.getJdbcTemplate().query(sqlCap.capture(), 
         		                             Matchers.any(RowMapper.class))).thenReturn(clusters);
		List<Cluster> results = tracker.getAllClusters("us-west-1", "us-west-2", "us-east-1");
		Assert.assertEquals(results.size(), 3);
		Assert.assertEquals(sqlCap.getValue().toString().trim(), "select * from conformitytable where cluster is not null and region in ('us-west-1','us-west-2','us-east-1')");
    }
    
	@SuppressWarnings("unchecked")
	@Test
    public void testGetClusterNotFound() {
        ArrayList<Cluster> clusters = new ArrayList<>();
        TestRDSConformityClusterTracker tracker = new TestRDSConformityClusterTracker();
        
		when(tracker.getJdbcTemplate().query(Matchers.anyString(), 
         		                             Matchers.any(Object[].class), 
         		                             Matchers.any(RowMapper.class))).thenReturn(clusters);
		Cluster cluster = tracker.getCluster("clustername", "us-west-1");
		Assert.assertNull(cluster);
    }

}

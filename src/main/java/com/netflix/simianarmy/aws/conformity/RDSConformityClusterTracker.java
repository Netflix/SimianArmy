/*
 *
 *  Copyright 2013 Netflix, Inc.
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
package com.netflix.simianarmy.aws.conformity;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.ConformityClusterTracker;

/**
 * The RDSConformityClusterTracker implementation in RDS (relational database).
 */
public class RDSConformityClusterTracker implements ConformityClusterTracker {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RDSConformityClusterTracker.class);

    /** The table. */
    private final String table;
    
    /** the jdbcTemplate  */
    JdbcTemplate jdbcTemplate = null;
    
    /**
     * Instantiates a new RDS db resource tracker.
     *
     */
    public RDSConformityClusterTracker(String dbDriver, String dbUser,
			String dbPass, String dbUrl, String dbTable) {
    	DriverManagerDataSource dataSource = new DriverManagerDataSource(dbUrl, dbUser, dbPass);
    	dataSource.setDriverClassName(dbDriver);    	
    	this.jdbcTemplate = new JdbcTemplate(dataSource);
    	this.table = dbTable;
	}

    /**
     * Instantiates a new RDS conformity cluster tracker.  This constructor is intended
     * for unit testing.
     *
     */
    public RDSConformityClusterTracker(JdbcTemplate jdbcTemplate, String table) {
    	this.jdbcTemplate = jdbcTemplate;
    	this.table = table;
    }
    
    public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

    public Object value(String value) {
    	return value == null ? Types.NULL : value;
    }

    public Object value(Date value) {
    	return value == null ? Types.NULL : value.getTime() + "";
    }    

    public Object value(boolean value) {
    	return Boolean.toString(value);
    }
    
    /** {@inheritDoc} */
    @Override
    public void addOrUpdate(Cluster cluster) {
    	Cluster orig = getCluster(cluster.getName(), cluster.getRegion());    	
        LOGGER.debug(String.format("Saving cluster %s to RDB table %s", cluster.getName(), table));    	
    	String json;
		try {
			json = new ObjectMapper().writeValueAsString(cluster.getFieldToValueMap());
		} catch (JsonProcessingException e) {
			LOGGER.error("ERROR generating JSON when saving cluster " + cluster.getName(), e);
			return;
		}

    	if (orig == null) {
    		StringBuilder sb = new StringBuilder();
    		sb.append("insert into ").append(table);
    		sb.append(" (");
    		sb.append(Cluster.CLUSTER).append(",");
    		sb.append(Cluster.REGION).append(",");
    		sb.append(Cluster.OWNER_EMAIL).append(",");
    		sb.append(Cluster.IS_CONFORMING).append(",");
    		sb.append(Cluster.IS_OPTEDOUT).append(",");
    		sb.append(Cluster.UPDATE_TIMESTAMP).append(",");
    		sb.append("dataJson").append(") values (?,?,?,?,?,?,?)");

            LOGGER.debug(String.format("Insert statement is '%s'", sb));
    		this.jdbcTemplate.update(sb.toString(),
    								 cluster.getName(),
    								 cluster.getRegion(),
    								 value(cluster.getOwnerEmail()),
    								 value(cluster.isConforming()),
    								 value(cluster.isOptOutOfConformity()),
    								 value(cluster.getUpdateTime()),
    								 json);    	
    	} else {
    		StringBuilder sb = new StringBuilder();
    		sb.append("update ").append(table).append(" set ");
    		sb.append(Cluster.OWNER_EMAIL).append("=?,");
    		sb.append(Cluster.IS_CONFORMING).append("=?,");
    		sb.append(Cluster.IS_OPTEDOUT).append("=?,");
    		sb.append(Cluster.UPDATE_TIMESTAMP).append("=?,");
    		sb.append("dataJson").append("=? where ");
    		sb.append(Cluster.CLUSTER).append("=? and ");
    		sb.append(Cluster.REGION).append("=?");

            LOGGER.debug(String.format("Insert statement is '%s'", sb));
    		this.jdbcTemplate.update(sb.toString(),
									 value(cluster.getOwnerEmail()),
									 value(cluster.isConforming()),
									 value(cluster.isOptOutOfConformity()),
									 value(cluster.getUpdateTime()),
									 json,
									 cluster.getName(),
									 cluster.getRegion());    	
    	}
    	LOGGER.debug("Successfully saved.");
    }

    /**
     * Gets the clusters for a list of regions. If the regions parameter is empty, returns the clusters
     * for all regions.
     */
    @Override
    public List<Cluster> getAllClusters(String... regions) {
        return getClusters(null, regions);
    }

    @Override
    public List<Cluster> getNonconformingClusters(String... regions) {
        return getClusters(false, regions);
    }

    @Override
    public Cluster getCluster(String clusterName, String region) {
        Validate.notEmpty(clusterName);
        Validate.notEmpty(region);
        StringBuilder query = new StringBuilder();
        query.append(String.format("select * from %s where cluster = ? and region = ?", table));
        LOGGER.info(String.format("Query is '%s'", query));

        List<Cluster> clusters = jdbcTemplate.query(query.toString(), new String[] {clusterName, region}, new RowMapper<Cluster>() {
            public Cluster mapRow(ResultSet rs, int rowNum) throws SQLException {
            	return mapResource(rs);                
            }             
        });                
        Validate.isTrue(clusters.size() <= 1);
        if (clusters.size() == 0) {
            LOGGER.info(String.format("Not found cluster with name %s in region %s", clusterName, region));
            return null;
        } else {
            Cluster cluster = clusters.get(0);
            return cluster;
        }
    }
    
    private Cluster mapResource(ResultSet rs) throws SQLException {
    	String json = rs.getString("dataJson");
    	ObjectMapper mapper = new ObjectMapper();
    	Cluster cluster = null;
    	try {
    		Map<String, String> map = mapper.readValue(json, Map.class);
    		cluster = cluster.parseFieldToValueMap(map);
    	}catch(IOException ie) {
    		LOGGER.error("Error parsing resource from json", ie);
    	}    	
        return cluster;
    }                 

    @Override
    public void deleteClusters(Cluster... clusters) {
        Validate.notNull(clusters);
        LOGGER.info(String.format("Deleting %d clusters", clusters.length));
        for (Cluster cluster : clusters) {
            LOGGER.info(String.format("Deleting cluster %s", cluster.getName()));
            String stmt = String.format("delete from %s where %s=? and %s=?", table, Cluster.CLUSTER, Cluster.REGION);
            jdbcTemplate.update(stmt, cluster.getName(), cluster.getRegion());
            LOGGER.info(String.format("Successfully deleted cluster %s", cluster.getName()));
        }
    }

    private List<Cluster> getClusters(Boolean conforming, String... regions) {
        Validate.notNull(regions);
        StringBuilder query = new StringBuilder();
        query.append(String.format("select * from %s where cluster is not null and ", table));
        boolean needsAnd = false;
        if (regions.length != 0) {
            query.append(String.format("region in ('%s') ", StringUtils.join(regions, "','")));
            needsAnd = true;
        }
        if (conforming != null) {
            if (needsAnd) {
                query.append(" and ");
            }
            query.append(String.format("isConforming = '%s'", conforming));
        }

        LOGGER.info(String.format("Query to retrieve clusters for regions %s is '%s'",
                StringUtils.join(regions, "','"), query.toString()));

        List<Cluster> clusters = jdbcTemplate.query(query.toString(), new RowMapper<Cluster>() {
            public Cluster mapRow(ResultSet rs, int rowNum) throws SQLException {
            	return mapResource(rs);                
            }             
        });                
        
        LOGGER.info(String.format("Retrieved %d clusters from RDS DB in table %s and regions %s",
                clusters.size(), table, StringUtils.join(regions, "','")));
        return clusters;
    }
    
    /**
     * Creates the RDS table, if it does not already exist.
     */
    public void init() {
        try {
            LOGGER.info("Creating RDS table: {}", table);
            String sql = String.format("create table if not exists %s ("
                                     + " %s varchar(255),"
                                     + " %s varchar(25),"
                                     + " %s varchar(255),"
                                     + " %s varchar(10),"
                                     + " %s varchar(10),"
                                     + " %s BIGINT," 
                                     + " %s varchar(4096) )",
                                     table,
                                     Cluster.CLUSTER,
                                     Cluster.REGION,
                                     Cluster.OWNER_EMAIL,
                                     Cluster.IS_CONFORMING,
                                     Cluster.IS_OPTEDOUT,
                                     Cluster.UPDATE_TIMESTAMP,
                                     "dataJson");
           LOGGER.debug("Create SQL is: '{}'", sql);
           jdbcTemplate.execute(sql);
            
        } catch (AmazonClientException e) {
            LOGGER.warn("Error while trying to auto-create RDS table", e);
        }
    }    
}
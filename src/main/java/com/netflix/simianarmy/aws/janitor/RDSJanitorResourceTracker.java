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
package com.netflix.simianarmy.aws.janitor;

import com.amazonaws.AmazonClientException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.Resource.CleanupState;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.janitor.JanitorResourceTracker;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * The JanitorResourceTracker implementation in AWS RDS.
 */
public class RDSJanitorResourceTracker implements JanitorResourceTracker {

    /** The Constant LOGGER. */
    public static final Logger LOGGER = LoggerFactory.getLogger(RDSJanitorResourceTracker.class);

    /** The table. */
    private final String table;
    
    /** the jdbcTemplate  */
    JdbcTemplate jdbcTemplate = null;
    
    /**
     * Instantiates a new RDS janitor resource tracker.
     *
     */
    public RDSJanitorResourceTracker(String dbDriver, String dbUser,
			String dbPass, String dbUrl, String dbTable) {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setDriverClassName(dbDriver);
		dataSource.setJdbcUrl(dbUrl);
		dataSource.setUsername(dbUser);
		dataSource.setPassword(dbPass);
		dataSource.setMaximumPoolSize(2);
    	this.jdbcTemplate = new JdbcTemplate(dataSource);
    	this.table = dbTable;
	}
    
    /**
     * Instantiates a new RDS janitor resource tracker.  This constructor is intended
     * for unit testing.
     *
     */
    public RDSJanitorResourceTracker(JdbcTemplate jdbcTemplate, String table) {
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
    	return value == null ? Types.NULL : value.getTime();
    }

	public Object value(boolean value) {
		return new Boolean(value).toString();
	}

	public Object emailValue(String email) {
		if (StringUtils.isBlank(email)) return Types.NULL;
		if (email.equals("0")) return Types.NULL;
		return email;
	}

	/** {@inheritDoc} */
    @Override
    public void addOrUpdate(Resource resource) {
    	Resource orig = getResource(resource.getId(), resource.getRegion());
        LOGGER.debug(String.format("Saving resource %s to RDB table %s in region %s", resource.getId(), table, resource.getRegion()));
    	String json;
		try {
			json = new ObjectMapper().writeValueAsString(additionalFieldsAsMap(resource));
		} catch (JsonProcessingException e) {
			LOGGER.error("ERROR generating additional field JSON when saving resource " + resource.getId(), e);
			return;
		}

    	if (orig == null) {
    		StringBuilder sb = new StringBuilder();
    		sb.append("insert into ").append(table);
    		sb.append(" (");
    		sb.append(AWSResource.FIELD_RESOURCE_ID).append(",");
    		sb.append(AWSResource.FIELD_RESOURCE_TYPE).append(",");
    		sb.append(AWSResource.FIELD_REGION).append(",");
    		sb.append(AWSResource.FIELD_OWNER_EMAIL).append(",");
    		sb.append(AWSResource.FIELD_DESCRIPTION).append(",");
    		sb.append(AWSResource.FIELD_STATE).append(",");
    		sb.append(AWSResource.FIELD_TERMINATION_REASON).append(",");
    		sb.append(AWSResource.FIELD_EXPECTED_TERMINATION_TIME).append(",");
    		sb.append(AWSResource.FIELD_ACTUAL_TERMINATION_TIME).append(",");
			sb.append(AWSResource.FIELD_NOTIFICATION_TIME).append(",");
    		sb.append(AWSResource.FIELD_LAUNCH_TIME).append(",");
    		sb.append(AWSResource.FIELD_MARK_TIME).append(",");
			sb.append(AWSResource.FIELD_OPT_OUT_OF_JANITOR).append(",");
    		sb.append("additionalFields").append(") values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");

            LOGGER.debug(String.format("Insert statement is '%s'", sb));
    		int updated = this.jdbcTemplate.update(sb.toString(),
    								 resource.getId(),
    								 value(resource.getResourceType().toString()),
    								 value(resource.getRegion()),
    								 emailValue(resource.getOwnerEmail()),
    								 value(resource.getDescription()),
    								 value(resource.getState().toString()),
    								 value(resource.getTerminationReason()),
    								 value(resource.getExpectedTerminationTime()),
    								 value(resource.getActualTerminationTime()),
					                 value(resource.getNotificationTime()),
    								 value(resource.getLaunchTime()),
    								 value(resource.getMarkTime()),
				  					 value(resource.isOptOutOfJanitor()),
    								 json);
            LOGGER.debug(String.format("%d rows inserted", updated));
    	} else {
    		StringBuilder sb = new StringBuilder();
    		sb.append("update ").append(table).append(" set ");
    		sb.append(AWSResource.FIELD_RESOURCE_TYPE).append("=?,");
    		sb.append(AWSResource.FIELD_REGION).append("=?,");
    		sb.append(AWSResource.FIELD_OWNER_EMAIL).append("=?,");
    		sb.append(AWSResource.FIELD_DESCRIPTION).append("=?,");
    		sb.append(AWSResource.FIELD_STATE).append("=?,");
    		sb.append(AWSResource.FIELD_TERMINATION_REASON).append("=?,");
    		sb.append(AWSResource.FIELD_EXPECTED_TERMINATION_TIME).append("=?,");
    		sb.append(AWSResource.FIELD_ACTUAL_TERMINATION_TIME).append("=?,");
			sb.append(AWSResource.FIELD_NOTIFICATION_TIME).append("=?,");
    		sb.append(AWSResource.FIELD_LAUNCH_TIME).append("=?,");
    		sb.append(AWSResource.FIELD_MARK_TIME).append("=?,");
			sb.append(AWSResource.FIELD_OPT_OUT_OF_JANITOR).append("=?,");
    		sb.append("additionalFields").append("=? where ");
    		sb.append(AWSResource.FIELD_RESOURCE_ID).append("=? and ");
			sb.append(AWSResource.FIELD_REGION).append("=?");

            LOGGER.debug(String.format("Update statement is '%s'", sb));
    		int updated = this.jdbcTemplate.update(sb.toString(),
    								 resource.getResourceType().toString(),
    								 value(resource.getRegion()),
					                 emailValue(resource.getOwnerEmail()),
    								 value(resource.getDescription()),
    								 value(resource.getState().toString()),
    								 value(resource.getTerminationReason()),
    								 value(resource.getExpectedTerminationTime()),
    								 value(resource.getActualTerminationTime()),
					                 value(resource.getNotificationTime()),
    								 value(resource.getLaunchTime()),
    								 value(resource.getMarkTime()),
					                 value(resource.isOptOutOfJanitor()),
    								 json,
    								 resource.getId(),
						  			 resource.getRegion());
            LOGGER.debug(String.format("%d rows updated", updated));
    	}
    	LOGGER.debug("Successfully saved.");
    }

	/**
     * Returns a list of AWSResource objects. You need to override this method if more
     * specific resource types (e.g. subtypes of AWSResource) need to be obtained from
     * the Database.
     */
    @Override
    public List<Resource> getResources(ResourceType resourceType, CleanupState state, String resourceRegion) {
        Validate.notEmpty(resourceRegion);
        StringBuilder query = new StringBuilder();
        ArrayList<String> args = new ArrayList<>();
        query.append(String.format("select * from %s where ", table));
        if (resourceType != null) {
            query.append("resourceType=? and ");
            args.add(resourceType.toString());
        }
        if (state != null) {
            query.append("state=? and ");
            args.add(state.toString());
        }
        query.append("region=?");
        args.add(resourceRegion);

        LOGGER.debug(String.format("Query is '%s'", query));
        List<Resource> resources = jdbcTemplate.query(query.toString(), args.toArray(), new RowMapper<Resource>() {
            public Resource mapRow(ResultSet rs, int rowNum) throws SQLException {
            	return mapResource(rs);                
            }             
        });                
        return resources;
    }
    
    private Resource mapResource(ResultSet rs) throws SQLException {
    	String json = rs.getString("additionalFields");
    	Resource resource = null;
    	try {
    		// put additional fields
    		Map<String, String> map = new HashMap<>();
    		if (json != null) {
            	TypeReference<HashMap<String,String>> typeRef = new TypeReference<HashMap<String,String>>() {};    		
        		map = new ObjectMapper().readValue(json, typeRef);
    		}
    		
    		// put everything else
    		map.put(AWSResource.FIELD_RESOURCE_ID, rs.getString(AWSResource.FIELD_RESOURCE_ID));
    		map.put(AWSResource.FIELD_RESOURCE_TYPE, rs.getString(AWSResource.FIELD_RESOURCE_TYPE));
    		map.put(AWSResource.FIELD_REGION, rs.getString(AWSResource.FIELD_REGION));
    		map.put(AWSResource.FIELD_DESCRIPTION, rs.getString(AWSResource.FIELD_DESCRIPTION));
    		map.put(AWSResource.FIELD_STATE, rs.getString(AWSResource.FIELD_STATE));
    		map.put(AWSResource.FIELD_TERMINATION_REASON, rs.getString(AWSResource.FIELD_TERMINATION_REASON));
			map.put(AWSResource.FIELD_OPT_OUT_OF_JANITOR, rs.getString(AWSResource.FIELD_OPT_OUT_OF_JANITOR));

			String email = rs.getString(AWSResource.FIELD_OWNER_EMAIL);
			if (StringUtils.isBlank(email) || email.equals("0")) {
				email = null;
			}
			map.put(AWSResource.FIELD_OWNER_EMAIL, email);

    		String expectedTerminationTime = millisToFormattedDate(rs.getString(AWSResource.FIELD_EXPECTED_TERMINATION_TIME));
    		String actualTerminationTime = millisToFormattedDate(rs.getString(AWSResource.FIELD_ACTUAL_TERMINATION_TIME));
			String notificationTime = millisToFormattedDate(rs.getString(AWSResource.FIELD_NOTIFICATION_TIME));
    		String launchTime = millisToFormattedDate(rs.getString(AWSResource.FIELD_LAUNCH_TIME));
    		String markTime = millisToFormattedDate(rs.getString(AWSResource.FIELD_MARK_TIME));

    		if (expectedTerminationTime != null) {
    			map.put(AWSResource.FIELD_EXPECTED_TERMINATION_TIME, expectedTerminationTime);
    		}
    		if (actualTerminationTime != null) {
    			map.put(AWSResource.FIELD_ACTUAL_TERMINATION_TIME, actualTerminationTime);
    		}
			if (notificationTime != null) {
				map.put(AWSResource.FIELD_NOTIFICATION_TIME, notificationTime);
			}
    		if (launchTime != null) {
    			map.put(AWSResource.FIELD_LAUNCH_TIME, launchTime);
    		}
    		if (markTime != null) {
    			map.put(AWSResource.FIELD_MARK_TIME, markTime);
    		}

    		resource = AWSResource.parseFieldtoValueMap(map);
    	}catch(IOException ie) {
    		String msg = "Error parsing resource from result set";
    		LOGGER.error(msg, ie);
    		throw new SQLException(msg);
    	}    	
        return resource;
    }

	private String millisToFormattedDate(String millisStr) {
		String datetime = null;
		try {
			long millis = Long.parseLong(millisStr);
			datetime = AWSResource.DATE_FORMATTER.print(millis);
		} catch(NumberFormatException nfe) {
			LOGGER.error(String.format("Error parsing datetime %s when reading from RDS", millisStr));
		}
		return datetime;
	}

    @Override
    public Resource getResource(String resourceId) {
        Validate.notEmpty(resourceId);
        StringBuilder query = new StringBuilder();
        query.append(String.format("select * from %s where resourceId=?", table));

        LOGGER.debug(String.format("Query is '%s'", query));
        List<Resource> resources = jdbcTemplate.query(query.toString(), new String[]{resourceId}, new RowMapper<Resource>() {
            public Resource mapRow(ResultSet rs, int rowNum) throws SQLException {
            	return mapResource(rs);                
            }             
        });       
        
        Resource resource = null;
        Validate.isTrue(resources.size() <= 1);
        if (resources.size() == 0) {
            LOGGER.info(String.format("Not found resource with id %s", resourceId));
        } else {
        	resource = resources.get(0);
        }
        return resource;
    }

	@Override
	public Resource getResource(String resourceId, String region) {
		Validate.notEmpty(resourceId);
		Validate.notEmpty(region);
		StringBuilder query = new StringBuilder();
		query.append(String.format("select * from %s where resourceId=? and region=?", table));

		LOGGER.debug(String.format("Query is '%s'", query));
		List<Resource> resources = jdbcTemplate.query(query.toString(), new String[]{resourceId,region}, new RowMapper<Resource>() {
			public Resource mapRow(ResultSet rs, int rowNum) throws SQLException {
				return mapResource(rs);
			}
		});

		Resource resource = null;
		Validate.isTrue(resources.size() <= 1);
		if (resources.size() == 0) {
			LOGGER.info(String.format("Not found resource with id %s", resourceId));
		} else {
			resource = resources.get(0);
		}
		return resource;
	}

	/**
     * Creates the RDS table, if it does not already exist.
     */
    public void init() {
        try {
            LOGGER.info("Creating RDS table: {}", table);
            String sql = String.format("create table if not exists %s ("
                                     + " %s varchar(255), "
                                     + " %s varchar(255), "
                                     + " %s varchar(25), "
                                     + " %s varchar(255), "
                                     + " %s varchar(255), "
                                     + " %s varchar(25), "
                                     + " %s varchar(255), "
                                     + " %s BIGINT, " 
                                     + " %s BIGINT, " 
                                     + " %s BIGINT, "
							         + " %s BIGINT, "
					                 + " %s BIGINT, "
					                 + " %s varchar(8), "
                                     + " %s varchar(4096) )",
                                     table,
                                     AWSResource.FIELD_RESOURCE_ID,
                                     AWSResource.FIELD_RESOURCE_TYPE,
                                     AWSResource.FIELD_REGION,
                                     AWSResource.FIELD_OWNER_EMAIL,
                             		 AWSResource.FIELD_DESCRIPTION,
                                     AWSResource.FIELD_STATE,
                                     AWSResource.FIELD_TERMINATION_REASON,
                                     AWSResource.FIELD_EXPECTED_TERMINATION_TIME,
                                     AWSResource.FIELD_ACTUAL_TERMINATION_TIME,
					                 AWSResource.FIELD_NOTIFICATION_TIME,
                                     AWSResource.FIELD_LAUNCH_TIME,
                                     AWSResource.FIELD_MARK_TIME,
					                 AWSResource.FIELD_OPT_OUT_OF_JANITOR,
                                     "additionalFields");
            LOGGER.debug("Create SQL is: '{}'", sql);
            jdbcTemplate.execute(sql);
            
        } catch (AmazonClientException e) {
            LOGGER.warn("Error while trying to auto-create RDS table", e);
        }
    }    
    
    private HashMap<String, String> additionalFieldsAsMap(Resource resource) {
    	HashMap<String, String> fields = new HashMap<>();
    	for(String key : resource.getAdditionalFieldNames()) {
    		fields.put(key, resource.getAdditionalField(key));
    	}
		return fields;
	}    
}

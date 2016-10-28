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
package com.netflix.simianarmy.aws;

import com.amazonaws.AmazonClientException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.simianarmy.EventType;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyType;
import com.netflix.simianarmy.basic.BasicRecorderEvent;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The Class RDSRecorder. Records events to and fetched events from a RDS table (default SIMIAN_ARMY)
 */
@SuppressWarnings("serial")
public class RDSRecorder implements MonkeyRecorder {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RDSRecorder.class);

    private final String region;

    /** The table. */
    private final String table;
    
    /** the jdbcTemplate  */
    JdbcTemplate jdbcTemplate = null;

    public static final String FIELD_ID = "eventId";
    public static final String FIELD_EVENT_TIME = "eventTime";
    public static final String FIELD_MONKEY_TYPE = "monkeyType";
    public static final String FIELD_EVENT_TYPE = "eventType";
    public static final String FIELD_REGION = "region";
    public static final String FIELD_DATA_JSON = "dataJson";
        
    /**
     * Instantiates a new RDS recorder.
     *
     */
    public RDSRecorder(String dbDriver, String dbUser,
			String dbPass, String dbUrl, String dbTable, String region) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(dbDriver);
        dataSource.setJdbcUrl(dbUrl);
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPass);
        dataSource.setMaximumPoolSize(2);
    	this.jdbcTemplate = new JdbcTemplate(dataSource);
    	this.table = dbTable;
    	this.region = region;
    }

    /**
     * Instantiates a new RDS recorder.  This constructor is intended
     * for unit testing.
     *
     */
    public RDSRecorder(JdbcTemplate jdbcTemplate, String table, String region) {
    	this.jdbcTemplate = jdbcTemplate;
    	this.table = table;
    	this.region = region;
    }
    
    public JdbcTemplate getJdbcTemplate() {
		return jdbcTemplate;
	}

    /** {@inheritDoc} */
    @Override
    public Event newEvent(MonkeyType monkeyType, EventType eventType, String reg, String id) {
        return new BasicRecorderEvent(monkeyType, eventType, reg, id);
    }

    /** {@inheritDoc} */
    @Override
    public void recordEvent(Event evt) {    	
        String evtTime = String.valueOf(evt.eventTime().getTime());
        String name = String.format("%s-%s-%s-%s", evt.monkeyType().name(), evt.id(), region, evtTime);
    	String json;
		try {
			json = new ObjectMapper().writeValueAsString(evt.fields());
		} catch (JsonProcessingException e) {
			LOGGER.error("ERROR generating JSON when saving resource " + name, e);
			return;
		}
        
        LOGGER.debug(String.format("Saving event %s to RDS table %s", name, table));    	        
		StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(table);
		sb.append(" (");
		sb.append(FIELD_ID).append(",");
		sb.append(FIELD_EVENT_TIME).append(",");
		sb.append(FIELD_MONKEY_TYPE).append(",");
		sb.append(FIELD_EVENT_TYPE).append(",");
		sb.append(FIELD_REGION).append(",");
		sb.append(FIELD_DATA_JSON).append(") values (?,?,?,?,?,?)");

        LOGGER.debug(String.format("Insert statement is '%s'", sb));
        int updated = this.jdbcTemplate.update(sb.toString(),
											   evt.id(),
											   evt.eventTime().getTime(),
											   SimpleDBRecorder.enumToValue(evt.monkeyType()),
											   SimpleDBRecorder.enumToValue(evt.eventType()),
											   evt.region(),
											   json);    	
        LOGGER.debug(String.format("%d rows inserted", updated));        
    }

    /** {@inheritDoc} */
    @Override
    public List<Event> findEvents(Map<String, String> query, Date after) {
        return findEvents(null, null, query, after);
    }

    /** {@inheritDoc} */
    @Override
    public List<Event> findEvents(MonkeyType monkeyType, Map<String, String> query, Date after) {
        return findEvents(monkeyType, null, query, after);
    }

    /** {@inheritDoc} */
    @Override
    public List<Event> findEvents(MonkeyType monkeyType, EventType eventType, Map<String, String> query, Date after) {
        ArrayList<Object> args = new ArrayList<>();
        StringBuilder sqlquery = new StringBuilder(
                String.format("select * from %s where region = ?", table));
        args.add(region);
        
        if (monkeyType != null) {
        	sqlquery.append(String.format(" and %s = ?", FIELD_MONKEY_TYPE));
        	args.add(SimpleDBRecorder.enumToValue(monkeyType));
        }

        if (eventType != null) {
        	sqlquery.append(String.format(" and %s = ?", FIELD_EVENT_TYPE));
        	args.add(SimpleDBRecorder.enumToValue(eventType));
        }
        
        for (Map.Entry<String, String> pair : query.entrySet()) {
        	sqlquery.append(String.format(" and %s like ?", FIELD_DATA_JSON));
            args.add((String.format("%s: \"%s\"", pair.getKey(), pair.getValue())));
        }
        sqlquery.append(String.format(" and %s > ? order by %s desc", FIELD_EVENT_TIME, FIELD_EVENT_TIME));
        args.add(new Long(after.getTime()));
        
        LOGGER.debug(String.format("Query is '%s'", sqlquery));
        List<Event> events = jdbcTemplate.query(sqlquery.toString(), args.toArray(), new RowMapper<Event>() {
            public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
            	return mapEvent(rs);                
            }             
        });                
        return events;
    }
    
    private Event mapEvent(ResultSet rs) throws SQLException {
    	String json = rs.getString("dataJson");
    	ObjectMapper mapper = new ObjectMapper();
    	Event event = null;
    	try {
    		String id = rs.getString(FIELD_ID);
    		MonkeyType monkeyType = SimpleDBRecorder.valueToEnum(MonkeyType.class, rs.getString(FIELD_MONKEY_TYPE));
    		EventType eventType = SimpleDBRecorder.valueToEnum(EventType.class, rs.getString(FIELD_EVENT_TYPE));
    		String region = rs.getString(FIELD_REGION);
    		long time = rs.getLong(FIELD_EVENT_TIME);    		
    	    event = new BasicRecorderEvent(monkeyType, eventType, region, id, time);

            TypeReference<Map<String,String>> typeRef = new TypeReference<Map<String,String>>() {};
    		Map<String, String> map = mapper.readValue(json, typeRef);
    	    for(String key : map.keySet()) {
    	    	event.addField(key, map.get(key));
    	    }

    	}catch(IOException ie) {
    		LOGGER.error("Error parsing resource from json", ie);
    	}    	
        return event;
    }             
                   

    /**
     * Creates the RDS table, if it does not already exist.
     */
    public void init() {
        try {
            if (this.region == null || this.region.equals("region-null")) {
                // This is a mock with an invalid region; avoid a slow timeout
                LOGGER.debug("Region=null; skipping RDS table creation");
                return;
            }
            
            LOGGER.info("Creating RDS table: {}", table);
            String sql = String.format("create table if not exists %s ("
                                     + " %s varchar(255),"
                                     + " %s BIGINT,"
                                     + " %s varchar(255),"
                                     + " %s varchar(255),"
                                     + " %s varchar(255),"
                                     + " %s varchar(4096) )", 
                                     table, 
                                     FIELD_ID, 
                                     FIELD_EVENT_TIME, 
                                     FIELD_MONKEY_TYPE, 
                                     FIELD_EVENT_TYPE, 
                                     FIELD_REGION, 
                                     FIELD_DATA_JSON);
            LOGGER.debug("Create SQL is: '{}'", sql);
            jdbcTemplate.execute(sql);
            
        } catch (AmazonClientException e) {
            LOGGER.warn("Error while trying to auto-create RDS table", e);
        }
    }
}

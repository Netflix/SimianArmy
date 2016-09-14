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

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.*;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.Resource.CleanupState;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.janitor.JanitorResourceTracker;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The JanitorResourceTracker implementation in SimpleDB.
 */
public class SimpleDBJanitorResourceTracker implements JanitorResourceTracker {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDBJanitorResourceTracker.class);

    /** The domain. */
    private final String domain;

    /** The SimpleDB client. */
    private final AmazonSimpleDB simpleDBClient;

    /**
     * Instantiates a new simple db resource tracker.
     *
     * @param awsClient
     *            the AWS Client
     * @param domain
     *            the domain
     */
    public SimpleDBJanitorResourceTracker(AWSClient awsClient, String domain) {
        this.domain = domain;
        this.simpleDBClient = awsClient.sdbClient();
    }

    /**
     * Gets the SimpleDB client.
     * @return the SimpleDB client
     */
    protected AmazonSimpleDB getSimpleDBClient() {
        return simpleDBClient;
    }

    /** {@inheritDoc} */
    @Override
    public void addOrUpdate(Resource resource) {
        List<ReplaceableAttribute> attrs = new ArrayList<ReplaceableAttribute>();
        Map<String, String> fieldToValueMap = resource.getFieldToValueMap();
        for (Map.Entry<String, String> entry : fieldToValueMap.entrySet()) {
            attrs.add(new ReplaceableAttribute(entry.getKey(), entry.getValue(), true));
        }
        PutAttributesRequest putReqest = new PutAttributesRequest(domain, getSimpleDBItemName(resource), attrs);
        LOGGER.debug(String.format("Saving resource %s to SimpleDB domain %s",
                resource.getId(), domain));
        this.simpleDBClient.putAttributes(putReqest);
        LOGGER.debug("Successfully saved.");
    }

    /**
     * Returns a list of AWSResource objects. You need to override this method if more
     * specific resource types (e.g. subtypes of AWSResource) need to be obtained from
     * the SimpleDB.
     */
    @Override
    public List<Resource> getResources(ResourceType resourceType, CleanupState state, String resourceRegion) {
        Validate.notEmpty(resourceRegion);
        List<Resource> resources = new ArrayList<Resource>();
        StringBuilder query = new StringBuilder();
        query.append(String.format("select * from `%s` where ", domain));
        if (resourceType != null) {
            query.append(String.format("resourceType='%s' and ", resourceType));
        }
        if (state != null) {
            query.append(String.format("state='%s' and ", state));
        }
        query.append(String.format("region='%s'", resourceRegion));

        LOGGER.debug(String.format("Query is '%s'", query));

        List<Item> items = querySimpleDBItems(query.toString());
        for (Item item : items) {
            try {
                resources.add(parseResource(item));
            } catch (Exception e) {
                // Ignore the item that cannot be parsed.
                LOGGER.error(String.format("SimpleDB item %s cannot be parsed into a resource.", item));
            }
        }
        LOGGER.info(String.format("Retrieved %d resources from SimpleDB in domain %s for resource type %s"
                + " and state %s and region %s",
                resources.size(), domain, resourceType, state, resourceRegion));
        return resources;
    }

    @Override
    public Resource getResource(String resourceId) {
        Validate.notEmpty(resourceId);
        StringBuilder query = new StringBuilder();
        query.append(String.format("select * from `%s` where resourceId = '%s'", domain, resourceId));

        LOGGER.debug(String.format("Query is '%s'", query));

        List<Item> items = querySimpleDBItems(query.toString());
        Validate.isTrue(items.size() <= 1);
        if (items.size() == 0) {
            LOGGER.info(String.format("Not found resource with id %s", resourceId));
            return null;
        } else {
            Resource resource = null;
            try {
                resource = parseResource(items.get(0));
            } catch (Exception e) {
                // Ignore the item that cannot be parsed.
                LOGGER.error(String.format("SimpleDB item %s cannot be parsed into a resource.", items.get(0)));
            }
            return resource;
        }
    }

    @Override
    public Resource getResource(String resourceId, String region) {
        Validate.notEmpty(resourceId);
        Validate.notEmpty(region);
        StringBuilder query = new StringBuilder();
        query.append(String.format("select * from `%s` where resourceId = '%s' and region = '%s'", domain, resourceId, region));

        LOGGER.debug(String.format("Query is '%s'", query));

        List<Item> items = querySimpleDBItems(query.toString());
        Validate.isTrue(items.size() <= 1);
        if (items.size() == 0) {
            LOGGER.info(String.format("Not found resource with id %s and region %s", resourceId, region));
            return null;
        } else {
            Resource resource = null;
            try {
                resource = parseResource(items.get(0));
            } catch (Exception e) {
                // Ignore the item that cannot be parsed.
                LOGGER.error(String.format("SimpleDB item %s cannot be parsed into a resource.", items.get(0)));
            }
            return resource;
        }
    }

    /**
     * Parses a SimpleDB item into an AWS resource.
     * @param item the item from SimpleDB
     * @return the AWSResource object for the SimpleDB item
     */
    protected Resource parseResource(Item item) {
        Map<String, String> fieldToValue = new HashMap<String, String>();
        for (Attribute attr : item.getAttributes()) {
            String name = attr.getName();
            String value = attr.getValue();
            if (name != null && value != null) {
                fieldToValue.put(name, value);
            }
        }
        return AWSResource.parseFieldtoValueMap(fieldToValue);
    }

    /**
     * Gets the unique SimpleDB item name for a resource. The subclass can override this
     * method to generate the item name differently.
     * @param resource
     * @return the SimpleDB item name for the resource
     */
    protected String getSimpleDBItemName(Resource resource) {
        return String.format("%s-%s-%s", resource.getResourceType().name(), resource.getId(), resource.getRegion());
    }

    private List<Item> querySimpleDBItems(String query) {
        Validate.notNull(query);
        String nextToken = null;
        List<Item> items = new ArrayList<Item>();
        do {
            SelectRequest request = new SelectRequest(query);
            request.setNextToken(nextToken);
            request.setConsistentRead(Boolean.TRUE);
            SelectResult result = this.simpleDBClient.select(request);
            items.addAll(result.getItems());
            nextToken = result.getNextToken();
        } while (nextToken != null);

        return items;
    }
}

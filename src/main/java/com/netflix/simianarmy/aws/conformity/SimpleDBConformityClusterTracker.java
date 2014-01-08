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

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.google.common.collect.Lists;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.ConformityClusterTracker;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The ConformityResourceTracker implementation in SimpleDB.
 */
public class SimpleDBConformityClusterTracker implements ConformityClusterTracker {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDBConformityClusterTracker.class);

    /** The domain. */
    private final String domain;

    /** The SimpleDB client. */
    private final AmazonSimpleDB simpleDBClient;

    private static final int  MAX_ATTR_SIZE = 1024;

    /**
     * Instantiates a new simple db cluster tracker for conformity monkey.
     *
     * @param awsClient
     *            the AWS Client
     * @param domain
     *            the domain
     */
    public SimpleDBConformityClusterTracker(AWSClient awsClient, String domain) {
        Validate.notNull(awsClient);
        Validate.notNull(domain);
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
    public void addOrUpdate(Cluster cluster) {
        List<ReplaceableAttribute> attrs = new ArrayList<ReplaceableAttribute>();
        Map<String, String> fieldToValueMap = cluster.getFieldToValueMap();
        for (Map.Entry<String, String> entry : fieldToValueMap.entrySet()) {
            attrs.add(new ReplaceableAttribute(entry.getKey(), StringUtils.left(entry.getValue(), MAX_ATTR_SIZE),
                    true));
        }
        PutAttributesRequest putReqest = new PutAttributesRequest(domain, getSimpleDBItemName(cluster), attrs);
        LOGGER.debug(String.format("Saving cluster %s to SimpleDB domain %s",
                cluster.getName(), domain));
        this.simpleDBClient.putAttributes(putReqest);
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
        query.append(String.format("select * from `%s` where cluster = '%s' and region = '%s'",
                domain, clusterName, region));

        LOGGER.info(String.format("Query is to get the cluster is '%s'", query));

        List<Item> items = querySimpleDBItems(query.toString());
        Validate.isTrue(items.size() <= 1);
        if (items.size() == 0) {
            LOGGER.info(String.format("Not found cluster with name %s in region %s", clusterName, region));
            return null;
        } else {
            Cluster cluster = null;
            try {
                cluster = parseCluster(items.get(0));
            } catch (Exception e) {
                // Ignore the item that cannot be parsed.
                LOGGER.error(String.format("SimpleDB item %s cannot be parsed into a cluster.", items.get(0)));
            }
            return cluster;
        }
    }

    @Override
    public void deleteClusters(Cluster... clusters) {
        Validate.notNull(clusters);
        LOGGER.info(String.format("Deleting %d clusters", clusters.length));
        for (Cluster cluster : clusters) {
            LOGGER.info(String.format("Deleting cluster %s", cluster.getName()));
            simpleDBClient.deleteAttributes(new DeleteAttributesRequest(domain, getSimpleDBItemName(cluster)));
            LOGGER.info(String.format("Successfully deleted cluster %s", cluster.getName()));
        }
    }

    private List<Cluster> getClusters(Boolean conforming, String... regions) {
        Validate.notNull(regions);
        List<Cluster> clusters = Lists.newArrayList();
        StringBuilder query = new StringBuilder();
        query.append(String.format("select * from `%s` where cluster is not null and ", domain));
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

        List<Item> items = querySimpleDBItems(query.toString());
        for (Item item : items) {
            try {
                clusters.add(parseCluster(item));
            } catch (Exception e) {
                // Ignore the item that cannot be parsed.
                LOGGER.error(String.format("SimpleDB item %s cannot be parsed into a cluster.", item), e);
            }
        }
        LOGGER.info(String.format("Retrieved %d clusters from SimpleDB in domain %s and regions %s",
                clusters.size(), domain, StringUtils.join(regions, "','")));
        return clusters;
    }

    /**
     * Parses a SimpleDB item into a cluster.
     * @param item the item from SimpleDB
     * @return the cluster for the SimpleDB item
     */
    protected Cluster parseCluster(Item item) {
        Map<String, String> fieldToValue = new HashMap<String, String>();
        for (Attribute attr : item.getAttributes()) {
            String name = attr.getName();
            String value = attr.getValue();
            if (name != null && value != null) {
                fieldToValue.put(name, value);
            }
        }
        return Cluster.parseFieldToValueMap(fieldToValue);
    }

    /**
     * Gets the unique SimpleDB item name for a cluster. The subclass can override this
     * method to generate the item name differently.
     * @param cluster
     * @return the SimpleDB item name for the cluster
     */
    protected String getSimpleDBItemName(Cluster cluster) {
        return String.format("%s-%s", cluster.getName(), cluster.getRegion());
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

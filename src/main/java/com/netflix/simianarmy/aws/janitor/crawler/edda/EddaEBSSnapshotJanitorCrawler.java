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
package com.netflix.simianarmy.aws.janitor.crawler.edda;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import com.netflix.simianarmy.client.edda.EddaClient;
import com.netflix.simianarmy.janitor.JanitorCrawler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The crawler to crawl AWS EBS snapshots for janitor monkey using Edda.
 */
public class EddaEBSSnapshotJanitorCrawler implements JanitorCrawler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EddaEBSSnapshotJanitorCrawler.class);

    /** The name representing the additional field name of AMIs generated using the snapshot. */
    public static final String SNAPSHOT_FIELD_AMIS = "AMIs";


    /** The map from snapshot id to the AMI ids that are generated using the snapshot. */
    private final Map<String, Collection<String>> snapshotToAMIs = Maps.newHashMap();

    private final EddaClient eddaClient;
    private final List<String> regions = Lists.newArrayList();
    private final String defaultOwnerId;

    /**
     * The constructor.
     * @param defaultOwnerId
     *            the default owner id that snapshots need to have for being crawled, null means no filtering is
     *            needed
     * @param eddaClient
     *            the Edda client
     * @param regions
     *            the regions the crawler will crawl resources for
     */
    public EddaEBSSnapshotJanitorCrawler(String defaultOwnerId, EddaClient eddaClient, String... regions) {
        this.defaultOwnerId = defaultOwnerId;
        Validate.notNull(eddaClient);
        this.eddaClient = eddaClient;
        Validate.notNull(regions);
        for (String region : regions) {
            this.regions.add(region);
        }
    }

    @Override
    public EnumSet<? extends ResourceType> resourceTypes() {
        return EnumSet.of(AWSResourceType.EBS_SNAPSHOT);
    }

    @Override
    public List<Resource> resources(ResourceType resourceType) {
        if ("EBS_SNAPSHOT".equals(resourceType.name())) {
            return getSnapshotResources();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Resource> resources(String... resourceIds) {
        return getSnapshotResources(resourceIds);
    }

    private List<Resource> getSnapshotResources(String... snapshotIds) {
        List<Resource> resources = Lists.newArrayList();
        for (String region : regions) {
            resources.addAll(getSnapshotResourcesInRegion(region, snapshotIds));
        }
        return resources;
    }

    private List<Resource> getSnapshotResourcesInRegion(String region, String... snapshotIds) {
        refreshSnapshotToAMIs(region);

        String url = eddaClient.getBaseUrl(region) + "/aws/snapshots/";
        if (snapshotIds != null && snapshotIds.length != 0) {
            url += StringUtils.join(snapshotIds, ',');
            LOGGER.info(String.format("Getting snapshots in region %s for %d ids", region, snapshotIds.length));
        } else {
            LOGGER.info(String.format("Getting all snapshots in region %s", region));
        }
        url += ";state=completed;_expand:(snapshotId,state,description,startTime,tags,ownerId)";

        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get Jason node from edda for snapshots in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }

        List<Resource> resources = Lists.newArrayList();
        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            JsonNode elem = it.next();
            // Filter out shared snapshots that do not have the specified owner id.
            String ownerId = elem.get("ownerId").getTextValue();
            if (defaultOwnerId != null && !defaultOwnerId.equals(ownerId)) {
                LOGGER.info(String.format("Ignoring snapshotIds %s since it does not have the specified ownerId.",
                        elem.get("snapshotId").getTextValue()));
            } else {
                resources.add(parseJsonElementToSnapshotResource(region, elem));
            }
        }
        return resources;
    }

    private Resource parseJsonElementToSnapshotResource(String region, JsonNode jsonNode) {
        Validate.notNull(jsonNode);
        long startTime = jsonNode.get("startTime").asLong();

        Resource resource = new AWSResource().withId(jsonNode.get("snapshotId").getTextValue()).withRegion(region)
                .withResourceType(AWSResourceType.EBS_SNAPSHOT)
                .withLaunchTime(new Date(startTime));
        JsonNode tags = jsonNode.get("tags");

        if (tags == null || !tags.isArray() || tags.size() == 0) {
            LOGGER.debug(String.format("No tags is found for %s", resource.getId()));
        } else {
            for (Iterator<JsonNode> it = tags.getElements(); it.hasNext();) {
                JsonNode tag = it.next();
                String key = tag.get("key").getTextValue();
                String value = tag.get("value").getTextValue();
                resource.setTag(key, value);
            }
        }
        JsonNode description = jsonNode.get("description");
        if (description != null) {
            resource.setDescription(description.getTextValue());
        }
        ((AWSResource) resource).setAWSResourceState(jsonNode.get("state").getTextValue());
        Collection<String> amis = snapshotToAMIs.get(resource.getId());
        if (amis != null) {
            resource.setAdditionalField(SNAPSHOT_FIELD_AMIS, StringUtils.join(amis, ","));
        }
        resource.setOwnerEmail(getOwnerEmailForResource(resource));
        return resource;
    }


    @Override
    public String getOwnerEmailForResource(Resource resource) {
        Validate.notNull(resource);
        return resource.getTag(BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY);
    }

    /**
     * Gets the collection of AMIs that are created using a specific snapshot.
     * @param snapshotId the snapshot id
     */
    protected Collection<String> getAMIsForSnapshot(String snapshotId) {
        Collection<String> amis = snapshotToAMIs.get(snapshotId);
        if (amis != null) {
            return Collections.unmodifiableCollection(amis);
        } else {
            return Collections.emptyList();
        }
    }

    private void refreshSnapshotToAMIs(String region) {
        snapshotToAMIs.clear();

        LOGGER.info(String.format("Getting mapping from snapshot to AMIs in region %s", region));

        String url = eddaClient.getBaseUrl(region) + "/aws/images/"
                + ";_expand:(imageId,blockDeviceMappings:(ebs:(snapshotId)))";
        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get Jason node from edda for AMI mapping in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }

        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            JsonNode elem = it.next();
            String imageId = elem.get("imageId").getTextValue();
            JsonNode blockMappings = elem.get("blockDeviceMappings");
            if (blockMappings == null || !blockMappings.isArray() || blockMappings.size() == 0) {
                continue;
            }
            for (Iterator<JsonNode> blockMappingsIt = blockMappings.getElements(); blockMappingsIt.hasNext();) {
                JsonNode blockMappingNode = blockMappingsIt.next();
                JsonNode ebs = blockMappingNode.get("ebs");
                if (ebs == null) {
                    continue;
                }
                JsonNode snapshotIdNode = ebs.get("snapshotId");
                String snapshotId = snapshotIdNode.getTextValue();
                LOGGER.debug(String.format("Snapshot %s is used to generate AMI %s", snapshotId, imageId));

                Collection<String> amis = snapshotToAMIs.get(snapshotId);
                if (amis == null) {
                    amis = Lists.newArrayList();
                    snapshotToAMIs.put(snapshotId, amis);
                }
                amis.add(imageId);
            }
        }
    }
}

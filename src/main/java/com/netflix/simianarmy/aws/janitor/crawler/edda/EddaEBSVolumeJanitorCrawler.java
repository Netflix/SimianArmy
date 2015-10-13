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
import com.google.common.collect.Sets;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import com.netflix.simianarmy.client.edda.EddaClient;
import com.netflix.simianarmy.janitor.JanitorCrawler;
import com.netflix.simianarmy.janitor.JanitorMonkey;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * The crawler to crawl AWS EBS volumes for Janitor monkey using Edda.
 */
public class EddaEBSVolumeJanitorCrawler implements JanitorCrawler {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EddaEBSVolumeJanitorCrawler.class);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    private static final int BATCH_SIZE = 50;

    // The value below specifies how many days we want to look back in Edda to find the owner of old instances.
    // In case of Edda keeps too much history data, without a reasonable date range, the query may fail.
    private static final int LOOKBACK_DAYS = 90;

    /**
     * The field name for purpose.
     */
    public static final String PURPOSE = "purpose";

    /**
     * The field name for deleteOnTermination.
     */
    public static final String DELETE_ON_TERMINATION = "deleteOnTermination";

    /**
     * The field name for detach time.
     */
    public static final String DETACH_TIME = "detachTime";

    private final EddaClient eddaClient;
    private final List<String> regions = Lists.newArrayList();
    private final Map<String, String> instanceToOwner = Maps.newHashMap();

    /**
     * The constructor.
     * @param eddaClient
     *            the Edda client
     * @param regions
     *            the regions the crawler will crawl resources for
     */
    public EddaEBSVolumeJanitorCrawler(EddaClient eddaClient, String... regions) {
        Validate.notNull(eddaClient);
        this.eddaClient = eddaClient;
        Validate.notNull(regions);
        for (String region : regions) {
            this.regions.add(region);
            updateInstanceToOwner(region);
        }
        LOGGER.info(String.format("Found owner for %d instances in %s", instanceToOwner.size(), this.regions));
    }

    private void updateInstanceToOwner(String region) {
        LOGGER.info(String.format("Getting owners for all instances in region %s", region));

        long startTime = DateTime.now().minusDays(LOOKBACK_DAYS).getMillis();
        String url = String.format("%1$s/view/instances;_since=%2$d;state.name=running;tags.key=%3$s;"
                + "_expand:(instanceId,tags:(key,value))",
                eddaClient.getBaseUrl(region), startTime, BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY);
        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get Jason node from edda for instance owners in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }

        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            JsonNode elem = it.next();
            String instanceId = elem.get("instanceId").getTextValue();
            JsonNode tags = elem.get("tags");
            if (tags == null || !tags.isArray() || tags.size() == 0) {
                continue;
            }
            for (Iterator<JsonNode> tagsIt = tags.getElements(); tagsIt.hasNext();) {
                JsonNode tag = tagsIt.next();
                String tagKey = tag.get("key").getTextValue();
                if (BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY.equals(tagKey)) {
                    instanceToOwner.put(instanceId, tag.get("value").getTextValue());
                    break;
                }
            }
        }
    }

    @Override
    public EnumSet<? extends ResourceType> resourceTypes() {
        return EnumSet.of(AWSResourceType.EBS_VOLUME);
    }

    @Override
    public List<Resource> resources(ResourceType resourceType) {
        if ("EBS_VOLUME".equals(resourceType.name())) {
            return getVolumeResources();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Resource> resources(String... resourceIds) {
        return getVolumeResources(resourceIds);
    }

    @Override
    public String getOwnerEmailForResource(Resource resource) {
        Validate.notNull(resource);
        return resource.getTag(BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY);
    }

    private List<Resource> getVolumeResources(String... volumeIds) {
        List<Resource> resources = Lists.newArrayList();
        for (String region : regions) {
            resources.addAll(getUnattachedVolumeResourcesInRegion(region, volumeIds));
            addLastAttachmentInfo(resources);
        }
        return resources;
    }

    /**
     * Gets all volumes that are not attached to any instance. Janitor Monkey only considers unattached volumes
     * as cleanup candidates, so there is no need to get volumes that are in-use.
     * @param region
     * @return list of resources that are not attached to any instance
     */
    private List<Resource> getUnattachedVolumeResourcesInRegion(String region, String... volumeIds) {
        String url = eddaClient.getBaseUrl(region) + "/aws/volumes;";
        if (volumeIds != null && volumeIds.length != 0) {
            url += StringUtils.join(volumeIds, ',');
            LOGGER.info(String.format("Getting volumes in region %s for %d ids", region, volumeIds.length));
        } else {
            LOGGER.info(String.format("Getting all unattached volumes in region %s", region));
        }
        url += ";state=available;_expand:(volumeId,createTime,size,state,tags)";

        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get Jason node from edda for unattached volumes in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }

        List<Resource> resources = Lists.newArrayList();
        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            resources.add(parseJsonElementToVolumeResource(region, it.next()));
        }
        return resources;
    }

    private Resource parseJsonElementToVolumeResource(String region, JsonNode jsonNode) {
        Validate.notNull(jsonNode);
        long createTime = jsonNode.get("createTime").asLong();

        Resource resource = new AWSResource().withId(jsonNode.get("volumeId").getTextValue()).withRegion(region)
                .withResourceType(AWSResourceType.EBS_VOLUME)
                .withLaunchTime(new Date(createTime));

        JsonNode tags = jsonNode.get("tags");
        StringBuilder description = new StringBuilder();
        JsonNode size = jsonNode.get("size");
        description.append(String.format("size=%s", size == null ? "unknown" : size.getIntValue()));

        if (tags == null || !tags.isArray() || tags.size() == 0) {
            LOGGER.debug(String.format("No tags is found for %s", resource.getId()));
        } else {
            for (Iterator<JsonNode> it = tags.getElements(); it.hasNext();) {
                JsonNode tag = it.next();
                String key = tag.get("key").getTextValue();
                String value = tag.get("value").getTextValue();
                description.append(String.format("; %s=%s", key, value));
                resource.setTag(key, value);
                if (key.equals(PURPOSE)) {
                    resource.setAdditionalField(PURPOSE, value);
                }
            }
            resource.setDescription(description.toString());
        }
        ((AWSResource) resource).setAWSResourceState(jsonNode.get("state").getTextValue());
        return resource;
    }


    /**
     * Adds information of last attachment to the resources. To be compatible with the AWS implementation of
     * the same crawler, add the information to the JANITOR_META tag. It always uses the latest information
     * to update the tag in this resource (not writing back to AWS) no matter if the tag exists.
     * @param resources the volume resources
     */
    private void addLastAttachmentInfo(List<Resource> resources) {
        Validate.notNull(resources);
        LOGGER.info(String.format("Updating the latest attachment info for %d resources", resources.size()));
        Map<String, List<Resource>> regionToResources = Maps.newHashMap();
        for (Resource resource : resources) {
            List<Resource> regionalList = regionToResources.get(resource.getRegion());
            if (regionalList == null) {
                regionalList = Lists.newArrayList();
                regionToResources.put(resource.getRegion(), regionalList);
            }
            regionalList.add(resource);
        }
        for (Map.Entry<String, List<Resource>> entry : regionToResources.entrySet()) {
            LOGGER.info(String.format("Updating the latest attachment info for %d resources in region %s",
                    resources.size(), entry.getKey()));
            for (List<Resource> batch : Lists.partition(entry.getValue(), BATCH_SIZE)) {
                LOGGER.info(String.format("Processing batch of size %d", batch.size()));
                String batchUrl = getBatchUrl(entry.getKey(), batch);
                JsonNode batchResult = null;
                try {
                    batchResult = eddaClient.getJsonNodeFromUrl(batchUrl);
                } catch (IOException e) {
                    LOGGER.error("Failed to get response for the batch.", e);
                }
                Map<String, Resource> idToResource = Maps.newHashMap();
                for (Resource resource : batch) {
                    idToResource.put(resource.getId(), resource);

                }
                if (batchResult == null || !batchResult.isArray()) {
                    throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s",
                            batchUrl, batchResult));
                }

                Set<String> processedIds = Sets.newHashSet();
                for (Iterator<JsonNode> it = batchResult.getElements(); it.hasNext();) {
                    JsonNode elem = it.next();
                    JsonNode data = elem.get("data");
                    String volumeId = data.get("volumeId").getTextValue();
                    Resource resource = idToResource.get(volumeId);
                    JsonNode attachments = data.get("attachments");

                    if (!(attachments.isArray() && attachments.size() > 0)) {
                        continue;
                    }
                    JsonNode attachment = attachments.get(0);

                    JsonNode ltime = elem.get("ltime");
                    if (ltime == null || ltime.isNull()) {
                        continue;
                    }
                    DateTime detachTime = new DateTime(ltime.asLong());
                    processedIds.add(volumeId);
                    setAttachmentInfo(volumeId, attachment, detachTime, resource);
                }

                for (Map.Entry<String, Resource> volumeEntry : idToResource.entrySet()) {
                    String id = volumeEntry.getKey();
                    if (!processedIds.contains(id)) {
                        Resource resource = volumeEntry.getValue();
                        LOGGER.info(String.format("Volume %s never was attached, use createTime %s as the detachTime",
                                id, resource.getLaunchTime()));
                        setAttachmentInfo(id, null, new DateTime(resource.getLaunchTime().getTime()), resource);
                    }
                }
            }
        }
    }

    private void setAttachmentInfo(String volumeId, JsonNode attachment, DateTime detachTime, Resource resource) {
        String instanceId = null;
        if (attachment != null) {
            boolean deleteOnTermination = attachment.get(DELETE_ON_TERMINATION).getBooleanValue();
            if (deleteOnTermination) {
                LOGGER.info(String.format(
                        "Volume %s had set the deleteOnTermination flag as true", volumeId));
            }
            resource.setAdditionalField(DELETE_ON_TERMINATION, String.valueOf(deleteOnTermination));
            instanceId = attachment.get("instanceId").getTextValue();
        }
        // The subclass can customize the way to get the owner for a volume
        String owner = getOwnerEmailForResource(resource);
        if (owner == null && instanceId != null) {
            owner = instanceToOwner.get(instanceId);
        }
        resource.setOwnerEmail(owner);

        String metaTag = makeMetaTag(instanceId, owner, detachTime);
        LOGGER.info(String.format("Setting Janitor Metatag as %s for volume %s", metaTag, volumeId));
        resource.setTag(JanitorMonkey.JANITOR_META_TAG, metaTag);

        LOGGER.info(String.format("The last detach time of volume %s is %s", volumeId, detachTime));
        resource.setAdditionalField(DETACH_TIME, String.valueOf(detachTime.getMillis()));
    }

    private String makeMetaTag(String instance, String owner, DateTime lastDetachTime) {
        StringBuilder meta = new StringBuilder();
        meta.append(String.format("%s=%s;",
                JanitorMonkey.INSTANCE_TAG_KEY, instance == null ? "" : instance));
        meta.append(String.format("%s=%s;", BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY, owner == null ? "" : owner));
        meta.append(String.format("%s=%s", JanitorMonkey.DETACH_TIME_TAG_KEY,
                lastDetachTime == null ? "" : AWSResource.DATE_FORMATTER.print(lastDetachTime)));
        return meta.toString();
    }


    private String getBatchUrl(String region, List<Resource> batch) {
        StringBuilder batchUrl = new StringBuilder(eddaClient.getBaseUrl(region) + "/aws/volumes/");
        boolean isFirst = true;
        for (Resource resource : batch) {
            if (!isFirst) {
                batchUrl.append(',');
            } else {
                isFirst = false;
            }
            batchUrl.append(resource.getId());
        }
        batchUrl.append(";data.state=in-use;_since=0;_expand;_meta:"
                + "(ltime,data:(volumeId,attachments:(deleteOnTermination,instanceId)))");
        return batchUrl.toString();
    }
}

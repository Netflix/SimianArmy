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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The crawler to crawl AWS AMIs for janitor monkey using Edda. Only images that are not currently referenced
 * by any existing instances or launch configurations are returned.
 */
public class EddaImageJanitorCrawler implements JanitorCrawler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EddaImageJanitorCrawler.class);

    /** The name representing the additional field name for the last reference time by instance. */
    public static final String AMI_FIELD_LAST_INSTANCE_REF_TIME = "Last_Instance_Reference_Time";

    /** The name representing the additional field name for the last reference time by launch config. */
    public static final String AMI_FIELD_LAST_LC_REF_TIME = "Last_Launch_Config_Reference_Time";

    /** The name representing the additional field name for whether the image is a base image. **/
    public static final String AMI_FIELD_BASE_IMAGE = "Base_Image";

    private static final int BATCH_SIZE = 500;

    private final EddaClient eddaClient;
    private final List<String> regions = Lists.newArrayList();

    private final Set<String> usedByInstance = Sets.newHashSet();
    private final Set<String> usedByLaunchConfig = Sets.newHashSet();
    private final Set<String> usedNames = Sets.newHashSet();
    protected final Map<String, String> imageIdToName = Maps.newHashMap();
    private final Map<String, Long> imageIdToCreationTime = Maps.newHashMap();
    private final Set<String> ancestorImageIds = Sets.newHashSet();

    private String ownerId;
    private final int daysBack;

    private static final String IMAGE_ID = "ami-[a-z0-9]{8}";
    private static final Pattern BASE_AMI_ID_PATTERN = Pattern.compile("^.*?base_ami_id=(" + IMAGE_ID + ").*?");
    private static final Pattern ANCESTOR_ID_PATTERN = Pattern.compile("^.*?ancestor_id=(" + IMAGE_ID + ").*?$");

    /**
     * Instantiates a new basic AMI crawler.
     * @param eddaClient
     *            the Edda client
     * @param daysBack
     *            the number of days that the crawler checks back in history stored in Edda
     * @param regions
     *            the regions the crawler will crawl resources for
     */
    public EddaImageJanitorCrawler(EddaClient eddaClient, String ownerId, int daysBack, String... regions) {
        Validate.notNull(eddaClient);
        this.eddaClient = eddaClient;
        this.ownerId = ownerId;
        Validate.isTrue(daysBack >= 0);
        this.daysBack = daysBack;
        Validate.notNull(regions);
        for (String region : regions) {
            this.regions.add(region);
        }
    }

    @Override
    public EnumSet<? extends ResourceType> resourceTypes() {
        return EnumSet.of(AWSResourceType.IMAGE);
    }

    @Override
    public List<Resource> resources(ResourceType resourceType) {
        if ("IMAGE".equals(resourceType.name())) {
            return getAMIResources();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Resource> resources(String... imageIds) {
        return getAMIResources(imageIds);
    }

    @Override
    public String getOwnerEmailForResource(Resource resource) {
        Validate.notNull(resource);
        return resource.getTag(BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY);
    }

    private List<Resource> getAMIResources(String... imageIds) {
        refreshIdToNameMap();
        refreshAMIsUsedByInstance();
        refreshAMIsUsedByLC();
        refreshIdToCreationTime();
        for (String excludedId : getExcludedImageIds()) {
            String name = imageIdToName.get(excludedId);
            usedNames.add(name);
        }
        LOGGER.info(String.format("%d image names are used across the %d regions.",
                usedNames.size(), regions.size()));
        Collection<String> excludedImageIds = getExcludedImageIds();
        List<Resource> resources = Lists.newArrayList();
        for (String region : regions) {
            resources.addAll(getAMIResourcesInRegion(region, excludedImageIds, imageIds));
        }
        return resources;
    }

    /**
     * The method allows users to put their own logic to exclude a set of images from being
     * cleaned up by Janitor Monkey. In some cases, images are not used but still need to be
     * kept longer.
     * @return a collection of image ids that need to be excluded from Janitor Monkey
     */
    protected Collection<String> getExcludedImageIds() {
        return Sets.newHashSet();
    }

    private JsonNode getImagesInJson(String region, String... imageIds) {
        String url = eddaClient.getBaseUrl(region) + "/aws/images";
        if (imageIds != null && imageIds.length != 0) {
            url += StringUtils.join(imageIds, ',');
            LOGGER.info(String.format("Getting unreferenced AMIs in region %s for %d ids", region, imageIds.length));
        } else {
            LOGGER.info(String.format("Getting all unreferenced AMIs in region %s", region));
            if (StringUtils.isNotBlank(ownerId)) {
                url += ";ownerId=" + ownerId;
            }
        }
        url += ";_expand:(imageId,name,description,state,tags:(key,value))";

        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get Jason node from edda for AMIs in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }
        return jsonNode;
    }

    private void refreshIdToNameMap() {
        imageIdToName.clear();
        for (String region : regions) {
            JsonNode jsonNode = getImagesInJson(region);
            for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
                JsonNode ami = it.next();
                String imageId = ami.get("imageId").getTextValue();
                String name = ami.get("name").getTextValue();
                imageIdToName.put(imageId, name);
            }
        }
        LOGGER.info(String.format("Got mapping from image id to name for %d ids", imageIdToName.size()));
    }

    /**
     * AWS doesn't provide creation time for images. We use the ctime (the creation time of the image record in Edda)
     * to approximate the creation time of the image.
     */
    private void refreshIdToCreationTime() {
        for (String region : regions) {
            String url = eddaClient.getBaseUrl(region) + "/aws/images";
            LOGGER.info(String.format("Getting the creation time for all AMIs in region %s", region));
            if (StringUtils.isNotBlank(ownerId)) {
                url += ";data.ownerId=" + ownerId;
            }
            url += ";_expand;_meta:(ctime,data:(imageId))";

            JsonNode jsonNode = null;
            try {
                jsonNode = eddaClient.getJsonNodeFromUrl(url);
            } catch (Exception e) {
                LOGGER.error(String.format(
                        "Failed to get Jason node from edda for creation time of AMIs in region %s.", region), e);
            }

            if (jsonNode == null || !jsonNode.isArray()) {
                throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s",
                        url, jsonNode));
            }

            for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
                JsonNode elem = it.next();
                JsonNode data = elem.get("data");
                String imageId = data.get("imageId").getTextValue();
                JsonNode ctimeNode = elem.get("ctime");
                if (ctimeNode != null && !ctimeNode.isNull()) {
                    long ctime = ctimeNode.asLong();
                    LOGGER.debug(String.format("The image record of %s was created in Edda at %s",
                            imageId, new DateTime(ctime)));
                    imageIdToCreationTime.put(imageId, ctime);
                }
            }
        }
        LOGGER.info(String.format("Got creation time for %d images", imageIdToCreationTime.size()));
    }

    private List<Resource> getAMIResourcesInRegion(
            String region, Collection<String> excludedImageIds, String... imageIds) {
        JsonNode jsonNode = getImagesInJson(region, imageIds);
        List<Resource> resources = Lists.newArrayList();
        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            JsonNode ami = it.next();
            String imageId = ami.get("imageId").getTextValue();
            Resource resource = parseJsonElementToresource(region, ami);
            String name = ami.get("name").getTextValue();
            if (excludedImageIds.contains(imageId)) {
                LOGGER.info(String.format("Image %s is excluded from being managed by Janitor Monkey, ignore.",
                        imageId));
                continue;
            }
            if (usedByInstance.contains(imageId) || usedByLaunchConfig.contains(imageId)) {
                LOGGER.info(String.format("AMI %s is referenced by existing instance or launch configuration.",
                        imageId));
            } else {
                LOGGER.info(String.format("AMI %s is not referenced by existing instance or launch configuration.",
                        imageId));
                if (usedNames.contains(name)) {
                    LOGGER.info(String.format("The same AMI name %s is used in another region", name));
                } else {
                    resources.add(resource);
                }
            }
        }
        long since = DateTime.now().minusDays(daysBack).getMillis();
        addLastReferenceInfo(resources, since);

        // Mark the base AMIs that are used as the ancestor of other images
        for (Resource resource : resources) {
            if (ancestorImageIds.contains(resource.getId())) {
                resource.setAdditionalField(AMI_FIELD_BASE_IMAGE, "true");
            }
        }

        return resources;
    }

    private Resource parseJsonElementToresource(String region, JsonNode jsonNode) {
        Validate.notNull(jsonNode);

        String imageId = jsonNode.get("imageId").getTextValue();

        Resource resource = new AWSResource().withId(imageId).withRegion(region)
                .withResourceType(AWSResourceType.IMAGE);

        Long creationTime = imageIdToCreationTime.get(imageId);
        if (creationTime != null) {
            resource.setLaunchTime(new Date(creationTime));
        }

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

        JsonNode descNode = jsonNode.get("description");
        if (descNode != null && !descNode.isNull()) {
            String description = descNode.getTextValue();
            resource.setDescription(description);
            String ancestorImageId = getBaseAmiIdFromDescription(description);
            if (ancestorImageId != null && !ancestorImageIds.contains(ancestorImageId)) {
                LOGGER.info(String.format("Found base AMI id %s from description '%s'", ancestorImageId, description));
                ancestorImageIds.add(ancestorImageId);
            }
        }
        ((AWSResource) resource).setAWSResourceState(jsonNode.get("state").getTextValue());

        String owner = getOwnerEmailForResource(resource);
        if (owner != null) {
            resource.setOwnerEmail(owner);
        }
        return resource;
    }

    private void refreshAMIsUsedByInstance() {
        usedByInstance.clear();
        for (String region : regions) {
            LOGGER.info(String.format("Getting AMIs used by instances in region %s", region));
            String url = eddaClient.getBaseUrl(region) + "/view/instances/;_expand:(imageId)";

            JsonNode jsonNode = null;
            try {
                jsonNode = eddaClient.getJsonNodeFromUrl(url);
            } catch (Exception e) {
                LOGGER.error(String.format(
                        "Failed to get Jason node from edda for AMIs used by instances in region %s.", region), e);
            }

            if (jsonNode == null || !jsonNode.isArray()) {
                throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s",
                        url, jsonNode));
            }

            for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
                JsonNode img = it.next();
                String id = img.get("imageId").getTextValue();
                usedByInstance.add(id);
                usedNames.add(imageIdToName.get(id));
            }
        }
        LOGGER.info(String.format("Found %d image ids used by instance from Edda", usedByInstance.size()));
    }

    private void refreshAMIsUsedByLC() {
        usedByLaunchConfig.clear();
        for (String region : regions) {
            LOGGER.info(String.format("Getting AMIs used by launch configs in region %s", region));
            String url = eddaClient.getBaseUrl(region) + "/aws/launchConfigurations;_expand:(imageId)";

            JsonNode jsonNode = null;
            try {
                jsonNode = eddaClient.getJsonNodeFromUrl(url);
            } catch (Exception e) {
                LOGGER.error(String.format(
                        "Failed to get Jason node from edda for AMIs used by launch configs in region %s.", region), e);
            }

            if (jsonNode == null || !jsonNode.isArray()) {
                throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s",
                        url, jsonNode));
            }

            for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
                JsonNode img = it.next();
                String id = img.get("imageId").getTextValue();
                usedByLaunchConfig.add(id);
                usedNames.add(imageIdToName.get(id));
            }
        }
        LOGGER.info(String.format("Found %d image ids used by launch config from Edda", usedByLaunchConfig.size()));
    }

    private void addLastReferenceInfo(List<Resource> resources, long since) {
        Validate.notNull(resources);
        LOGGER.info(String.format("Updating the latest reference info for %d images", resources.size()));
        Map<String, List<Resource>> regionToResources = Maps.newHashMap();
        for (Resource resource : resources) {
            List<Resource> regionalList = regionToResources.get(resource.getRegion());
            if (regionalList == null) {
                regionalList = Lists.newArrayList();
                regionToResources.put(resource.getRegion(), regionalList);
            }
            regionalList.add(resource);
        }
        //
        for (Map.Entry<String, List<Resource>> entry : regionToResources.entrySet()) {
            String region = entry.getKey();
            LOGGER.info(String.format("Updating the latest reference info for %d images in region %s",
                    resources.size(), region));
            for (List<Resource> batch : Lists.partition(entry.getValue(), BATCH_SIZE)) {
                LOGGER.info(String.format("Processing batch of size %d", batch.size()));
                updateReferenceTimeByInstance(region, batch, since);
                updateReferenceTimeByLaunchConfig(region, batch, since);
            }
        }
    }

    private void updateReferenceTimeByInstance(String region, List<Resource> batch, long since) {
        LOGGER.info(String.format("Getting the last reference time by instance for batch of size %d", batch.size()));
        String batchUrl = getInstanceBatchUrl(region, batch, since);
        JsonNode batchResult = null;
        Map<String, Resource> idToResource = Maps.newHashMap();
        for (Resource resource : batch) {
            idToResource.put(resource.getId(), resource);
        }
        try {
            batchResult = eddaClient.getJsonNodeFromUrl(batchUrl);
        } catch (IOException e) {
            LOGGER.error("Failed to get response for the batch.", e);
        }
        if (batchResult == null || !batchResult.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s",
                    batchUrl, batchResult));
        }
        for (Iterator<JsonNode> it = batchResult.getElements(); it.hasNext();) {
            JsonNode elem = it.next();
            JsonNode data = elem.get("data");
            String imageId = data.get("imageId").getTextValue();
            String instanceId = data.get("instanceId").getTextValue();
            JsonNode ltimeNode = elem.get("ltime");
            if (ltimeNode != null && !ltimeNode.isNull()) {
                long ltime = ltimeNode.asLong();
                Resource ami = idToResource.get(imageId);
                String lastRefTimeByInstance = ami.getAdditionalField(
                        AMI_FIELD_LAST_INSTANCE_REF_TIME);
                if (lastRefTimeByInstance == null || Long.parseLong(lastRefTimeByInstance) < ltime) {
                    LOGGER.info(String.format("The last time that the image %s was referenced by instance %s is %d",
                            imageId, instanceId, ltime));
                    ami.setAdditionalField(AMI_FIELD_LAST_INSTANCE_REF_TIME, String.valueOf(ltime));
                }
            }
        }
    }

    private void updateReferenceTimeByLaunchConfig(String region, List<Resource> batch, long since) {
        LOGGER.info(String.format("Getting the last reference time by launch config for batch of size %d",
                batch.size()));
        String batchUrl = getLaunchConfigBatchUrl(region, batch, since);
        JsonNode batchResult = null;
        Map<String, Resource> idToResource = Maps.newHashMap();
        for (Resource resource : batch) {
            idToResource.put(resource.getId(), resource);
        }
        try {
            batchResult = eddaClient.getJsonNodeFromUrl(batchUrl);
        } catch (IOException e) {
            LOGGER.error("Failed to get response for the batch.", e);
        }
        if (batchResult == null || !batchResult.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s",
                    batchUrl, batchResult));
        }
        for (Iterator<JsonNode> it = batchResult.getElements(); it.hasNext();) {
            JsonNode elem = it.next();
            JsonNode data = elem.get("data");
            String imageId = data.get("imageId").getTextValue();
            String launchConfigurationName = data.get("launchConfigurationName").getTextValue();
            JsonNode ltimeNode = elem.get("ltime");
            if (ltimeNode != null && !ltimeNode.isNull()) {
                long ltime = ltimeNode.asLong();
                Resource ami = idToResource.get(imageId);
                String lastRefTimeByLC = ami.getAdditionalField(AMI_FIELD_LAST_LC_REF_TIME);
                if (lastRefTimeByLC == null || Long.parseLong(lastRefTimeByLC) < ltime) {
                    LOGGER.info(String.format(
                            "The last time that the image %s was referenced by launch config %s is %d",
                            imageId, launchConfigurationName, ltime));
                    ami.setAdditionalField(AMI_FIELD_LAST_LC_REF_TIME, String.valueOf(ltime));
                }
            }
        }
    }

    private String getInstanceBatchUrl(String region, List<Resource> batch, long since) {
        StringBuilder batchUrl = new StringBuilder(eddaClient.getBaseUrl(region)
                + "/view/instances/;data.imageId=");
        batchUrl.append(getImageIdsString(batch));
        batchUrl.append(String.format(";data.state.name=terminated;_since=%d;_expand;_meta:"
                + "(ltime,data:(imageId,instanceId))", since));
        return batchUrl.toString();
    }

    private String getLaunchConfigBatchUrl(String region, List<Resource> batch, long since) {
        StringBuilder batchUrl = new StringBuilder(eddaClient.getBaseUrl(region)
                + "/aws/launchConfigurations/;data.imageId=");
        batchUrl.append(getImageIdsString(batch));
        batchUrl.append(String.format(";_since=%d;_expand;_meta:(ltime,data:(imageId,launchConfigurationName))",
                since));
        return batchUrl.toString();
    }

    private String getImageIdsString(List<Resource> resources) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Resource resource : resources) {
            if (!isFirst) {
                sb.append(',');
            } else {
                isFirst = false;
            }
            sb.append(resource.getId());
        }
        return sb.toString();
    }

    private static String getBaseAmiIdFromDescription(String imageDescription) {
        // base_ami_id=ami-1eb75c77,base_ami_name=servicenet-roku-qadd.dc.81210.10.44
        Matcher matcher = BASE_AMI_ID_PATTERN.matcher(imageDescription);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        // store=ebs,ancestor_name=ebs-centosbase-x86_64-20101124,ancestor_id=ami-7b4eb912
        matcher = ANCESTOR_ID_PATTERN.matcher(imageDescription);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

}

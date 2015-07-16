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
package com.netflix.simianarmy.aws.janitor.crawler;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.aws.janitor.VolumeTaggingMonkey;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.janitor.JanitorMonkey;

/**
 * The crawler to crawl AWS EBS volumes for janitor monkey.
 */
public class EBSVolumeJanitorCrawler extends AbstractAWSJanitorCrawler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EBSVolumeJanitorCrawler.class);

    /**
     * The constructor.
     * @param awsClient the AWS client
     */
    public EBSVolumeJanitorCrawler(AWSClient awsClient) {
        super(awsClient);
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

    private List<Resource> getVolumeResources(String... volumeIds) {
        List<Resource> resources = new LinkedList<Resource>();

        AWSClient awsClient = getAWSClient();

        for (Volume volume : awsClient.describeVolumes(volumeIds)) {
            Resource volumeResource = new AWSResource().withId(volume.getVolumeId())
                    .withRegion(getAWSClient().region()).withResourceType(AWSResourceType.EBS_VOLUME)
                    .withLaunchTime(volume.getCreateTime());
            for (Tag tag : volume.getTags()) {
                LOGGER.info(String.format("Adding tag %s = %s to resource %s",
                        tag.getKey(), tag.getValue(), volumeResource.getId()));
                volumeResource.setTag(tag.getKey(), tag.getValue());
            }
            volumeResource.setOwnerEmail(getOwnerEmailForResource(volumeResource));
            volumeResource.setDescription(getVolumeDescription(volume));
            ((AWSResource) volumeResource).setAWSResourceState(volume.getState());
            resources.add(volumeResource);
        }
        return resources;
    }

    private String getVolumeDescription(Volume volume) {
        StringBuilder description = new StringBuilder();
        Integer size = volume.getSize();
        description.append(String.format("size=%s", size == null ? "unknown" : size));
        for (Tag tag : volume.getTags()) {
            description.append(String.format("; %s=%s", tag.getKey(), tag.getValue()));
        }
        return description.toString();
    }

    @Override
    public String getOwnerEmailForResource(Resource resource) {
        String owner = super.getOwnerEmailForResource(resource);
        if (owner == null) {
            // try to find the owner from Janitor Metadata tag set by the volume tagging monkey.
            Map<String, String> janitorTag = VolumeTaggingMonkey.parseJanitorMetaTag(resource.getTag(
                    JanitorMonkey.JANITOR_META_TAG));
            owner = janitorTag.get(BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY);
        }
        return owner;
    }


}

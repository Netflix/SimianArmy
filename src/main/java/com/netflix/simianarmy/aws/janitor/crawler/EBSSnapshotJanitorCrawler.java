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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Tag;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;

/**
 * The crawler to crawl AWS EBS snapshots for janitor monkey.
 */
public class EBSSnapshotJanitorCrawler extends AbstractAWSJanitorCrawler {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EBSSnapshotJanitorCrawler.class);

    /** The name representing the additional field name of AMIs generated using the snapshot. */
    public static final String SNAPSHOT_FIELD_AMIS = "AMIs";


    /** The map from snapshot id to the AMI ids that are generated using the snapshot. */
    private final Map<String, Collection<String>> snapshotToAMIs =
            new HashMap<String, Collection<String>>();

    /**
     * The constructor.
     * @param awsClient the AWS client
     */
    public EBSSnapshotJanitorCrawler(AWSClient awsClient) {
        super(awsClient);
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
        refreshSnapshotToAMIs();

        List<Resource> resources = new LinkedList<Resource>();
        AWSClient awsClient = getAWSClient();

        for (Snapshot snapshot : awsClient.describeSnapshots(snapshotIds)) {
            Resource snapshotResource = new AWSResource().withId(snapshot.getSnapshotId())
                    .withRegion(getAWSClient().region()).withResourceType(AWSResourceType.EBS_SNAPSHOT)
                    .withLaunchTime(snapshot.getStartTime()).withDescription(snapshot.getDescription());
            for (Tag tag : snapshot.getTags()) {
                LOGGER.debug(String.format("Adding tag %s = %s to resource %s",
                        tag.getKey(), tag.getValue(), snapshotResource.getId()));
                snapshotResource.setTag(tag.getKey(), tag.getValue());
            }
            snapshotResource.setOwnerEmail(getOwnerEmailForResource(snapshotResource));
            ((AWSResource) snapshotResource).setAWSResourceState(snapshot.getState());
            Collection<String> amis = snapshotToAMIs.get(snapshotResource.getId());
            if (amis != null) {
                snapshotResource.setAdditionalField(SNAPSHOT_FIELD_AMIS, StringUtils.join(amis, ","));
            }
            resources.add(snapshotResource);
        }
        return resources;
    }

    @Override
    public String getOwnerEmailForResource(Resource resource) {
        Validate.notNull(resource);
        String owner = resource.getTag(BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY);
        if (owner == null) {
            owner = super.getOwnerEmailForResource(resource);
        }
        return owner;
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

    private void refreshSnapshotToAMIs() {
        snapshotToAMIs.clear();
        for (Image image : getAWSClient().describeImages()) {
            for (BlockDeviceMapping bdm : image.getBlockDeviceMappings()) {
                EbsBlockDevice ebd = bdm.getEbs();
                if (ebd != null && ebd.getSnapshotId() != null) {
                    LOGGER.debug(String.format("Snapshot %s is used to generate AMI %s",
                            ebd.getSnapshotId(), image.getImageId()));
                    Collection<String> amis = snapshotToAMIs.get(ebd.getSnapshotId());
                    if (amis == null) {
                        amis = new ArrayList<String>();
                        snapshotToAMIs.put(ebd.getSnapshotId(), amis);
                    }
                    amis.add(image.getImageId());
                }
            }
        }
    }
}

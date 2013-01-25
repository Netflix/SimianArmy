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
package com.netflix.simianarmy.aws.janitor.rule.asg;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.janitor.crawler.ASGJanitorCrawler;

/**
 * The class is for checking whether an ASG has any active instance using Discovery/Eureka.
 * If Discovery/Eureka is enabled, it uses its service to check if the instances in the ASG are
 * registered and up there.
 */
public class DiscoveryASGInstanceValidator implements ASGInstanceValidator {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DiscoveryASGInstanceValidator.class);
    private final DiscoveryClient discoveryClient;

    /**
     * Constructor.
     * @param discoveryClient
     *          the client to access the Discovery/Eureka service for checking the status of instances.
     */
    public DiscoveryASGInstanceValidator(DiscoveryClient discoveryClient) {
        Validate.notNull(discoveryClient);
        this.discoveryClient = discoveryClient;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasActiveInstance(Resource resource) {
        String instanceIds = resource.getAdditionalField(ASGJanitorCrawler.ASG_FIELD_INSTANCES);
        String maxSizeStr = resource.getAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE);
        if (StringUtils.isBlank(instanceIds)) {
            if (maxSizeStr != null && Integer.parseInt(maxSizeStr) == 0) {
                // The ASG is empty when it has no instance and the max size of the ASG is 0.
                // If the max size is not 0, the ASG could probably be in the process of starting new instances.
                LOGGER.info(String.format("ASG %s is empty.", resource.getId()));
                return false;
            } else {
                LOGGER.info(String.format("ASG %s does not have instances but the max size is %s",
                        resource.getId(), maxSizeStr));
                return true;
            }
        }
        String[] instances = StringUtils.split(instanceIds, ",");
        LOGGER.debug(String.format("Checking if the %d instances in ASG %s are active.",
                instances.length, resource.getId()));
        for (String instanceId : instances) {
            if (isActiveInstance(instanceId)) {
                LOGGER.info(String.format("ASG %s has active instance.", resource.getId()));
                return true;
            }
        }
        LOGGER.info(String.format("ASG %s has no active instance.", resource.getId()));
        return false;
    }

    /**
     * Returns true if the instance is registered in Eureka/Discovery.
     * @param instanceId the instance id
     * @return true if the instance is active, false otherwise
     */
    private boolean isActiveInstance(String instanceId) {
        Validate.notNull(instanceId);
        LOGGER.debug(String.format("Checking if instance %s is active", instanceId));
        List<InstanceInfo> instanceInfos = discoveryClient.getInstancesById(instanceId);
        for (InstanceInfo info : instanceInfos) {
            InstanceStatus status = info.getStatus();
            if (status == InstanceStatus.UP || status == InstanceStatus.STARTING) {
                LOGGER.debug(String.format("Instance %s is active in Discovery.", instanceId));
                return true;
            }
        }
        LOGGER.debug(String.format("Instance %s is not active in Discovery.", instanceId));
        return false;
    }
}

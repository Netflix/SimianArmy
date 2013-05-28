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
package com.netflix.simianarmy.aws.conformity.rule;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * The class implementing a client to access Eureda for getting instance information that is used
 * by Conformity Monkey.
 */
public class BasicConformityEurekaClient implements ConformityEurekaClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicConformityEurekaClient.class);

    private final DiscoveryClient discoveryClient;

    /**
     * Constructor.
     * @param discoveryClient the client to access Discovery/Eureka service.
     */
    public BasicConformityEurekaClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @Override
    public boolean hasHealthCheckUrl(String region, String instanceId) {
        List<InstanceInfo> instanceInfos = discoveryClient.getInstancesById(instanceId);
        for (InstanceInfo info : instanceInfos) {
            Set<String> healthCheckUrls = info.getHealthCheckUrls();
            if (healthCheckUrls != null && !healthCheckUrls.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasStatusUrl(String region, String instanceId) {
        List<InstanceInfo> instanceInfos = discoveryClient.getInstancesById(instanceId);
        for (InstanceInfo info : instanceInfos) {
            String statusPageUrl = info.getStatusPageUrl();
            if (!StringUtils.isEmpty(statusPageUrl)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isHealthy(String region, String instanceId) {
        List<InstanceInfo> instanceInfos = discoveryClient.getInstancesById(instanceId);
        if (instanceInfos.isEmpty()) {
            LOGGER.info(String.format("Instance %s is not registered in Eureka in region %s.", instanceId, region));
            return false;
        } else {
            for (InstanceInfo info : instanceInfos) {
                InstanceInfo.InstanceStatus status = info.getStatus();
                if (!status.equals(InstanceInfo.InstanceStatus.UP)
                        && !status.equals(InstanceInfo.InstanceStatus.STARTING)) {
                    LOGGER.info(String.format("Instance %s is not healthy in Eureka with status %s.",
                            instanceId, status.name()));
                    return false;
                }
            }
        }
        return true;
    }
}

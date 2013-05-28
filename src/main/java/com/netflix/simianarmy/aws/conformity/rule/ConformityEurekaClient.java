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

/**
 * The interface for a client to access Eureka service to get the status of instances for Conformity Monkey.
 */
public interface ConformityEurekaClient {
    /**
     * Checks whether an instance has health check url in Eureka.
     * @param region the region of the instance
     * @param instanceId the instance id
     * @return true if the instance has health check url in Eureka, false otherwise.
     */
    boolean hasHealthCheckUrl(String region, String instanceId);

    /**
     * Checks whether an instance has status url in Eureka.
     * @param region the region of the instance
     * @param instanceId the instance id
     * @return true if the instance has status url in Eureka, false otherwise.
     */
    boolean hasStatusUrl(String region, String instanceId);

    /**
     * Checks whether an instance is healthy in Eureka.
     * @param region the region of the instance
     * @param instanceId the instance id
     * @return true if the instance is healthy in Eureka, false otherwise.
     */
    boolean isHealthy(String region, String instanceId);
}

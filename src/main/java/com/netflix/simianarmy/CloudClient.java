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
package com.netflix.simianarmy;

/**
 * The CloudClient interface. This abstractions provides the interface that the monkeys need to interact with
 * "the cloud".
 */
public interface CloudClient {

    /**
     * Terminate instance.
     *
     * @param instanceId
     *            the instance id
     */
    void terminateInstance(String instanceId);
}

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
package com.netflix.simianarmy.client.edda;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.client.MonkeyRestClient;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST client to access Edda to get the history of a cloud resource.
 */
public class EddaClient extends MonkeyRestClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EddaClient.class);

    private final MonkeyConfiguration config;
    /**
     * Constructor.
     * @param timeout the timeout in milliseconds
     * @param maxRetries the max number of retries
     * @param retryInterval the interval in milliseconds between retries
     * @param config the monkey configuration
     */
    public EddaClient(int timeout, int maxRetries, int retryInterval, MonkeyConfiguration config) {
        super(timeout, maxRetries, retryInterval);
        this.config = config;
    }

    @Override
    public String getBaseUrl(String region) {
        Validate.notEmpty(region);
        String baseUrl = config.getStr("simianarmy.janitor.edda.endpoint." + region);
        if (StringUtils.isBlank(baseUrl)) {
            LOGGER.error(String.format("No endpoint of Edda is found for region %s.", region));
        }
        return baseUrl;
    }
}

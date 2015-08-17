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

import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import org.apache.commons.lang.Validate;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.janitor.JanitorCrawler;

/**
 * The abstract class for crawler of AWS resources.
 */
public abstract class AbstractAWSJanitorCrawler implements JanitorCrawler {
    /** The AWS client. */
    private final AWSClient awsClient;

    /**
     * The constructor.
     * @param awsClient the AWS client used by the crawler.
     */
    public AbstractAWSJanitorCrawler(AWSClient awsClient) {
        Validate.notNull(awsClient);
        this.awsClient = awsClient;
    }

    /**
     * Gets the owner email from the resource's tag key set in GLOBAL_OWNER_TAGKEY.
     * @param resource the resource
     * @return the owner email specified in the resource's tags
     */
    @Override
    public String getOwnerEmailForResource(Resource resource) {
        Validate.notNull(resource);
        return resource.getTag(BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY);
    }

    /**
     * Gets the AWS client used by the crawler.
     * @return the AWS client used by the crawler.
     */
    protected AWSClient getAWSClient() {
        return awsClient;
    }

}

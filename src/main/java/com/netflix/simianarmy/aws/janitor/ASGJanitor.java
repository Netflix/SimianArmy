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
package com.netflix.simianarmy.aws.janitor;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.janitor.AbstractJanitor;

/**
 * The Janitor responsible for ASG cleanup.
 */
public class ASGJanitor extends AbstractJanitor {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJanitor.class);

    private final AWSClient awsClient;

    /**
     * Constructor.
     * @param awsClient the AWS client
     * @param ctx the context
     */
    public ASGJanitor(AWSClient awsClient, AbstractJanitor.Context ctx) {
        super(ctx, AWSResourceType.ASG);
        Validate.notNull(awsClient);
        this.awsClient = awsClient;
    }

    @Override
    protected void postMark(Resource resource) {
    }

    @Override
    protected void cleanup(Resource resource) {
        LOGGER.info(String.format("Deleting ASG %s", resource.getId()));
        awsClient.deleteAutoScalingGroup(resource.getId());
    }

    @Override
    protected void postCleanup(Resource resource) {
    }

}

/*
 *
 *  Copyright 2013 Justin Santa Barbara.
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
package com.netflix.simianarmy.chaos;

import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * Adds entries to /etc/hosts so that DynamoDB API endpoints are unreachable.
 */
public class FailDynamoDbChaosType extends ScriptChaosType {
    /**
     * Constructor.
     *
     * @param config
     *            Configuration to use
     */
    public FailDynamoDbChaosType(MonkeyConfiguration config) {
        super(config, "FailDynamoDb");
    }
}

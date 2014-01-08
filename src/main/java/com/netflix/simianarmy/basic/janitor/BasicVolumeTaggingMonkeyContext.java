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
package com.netflix.simianarmy.basic.janitor;

import com.google.common.collect.Lists;
import com.netflix.simianarmy.aws.janitor.VolumeTaggingMonkey;
import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import com.netflix.simianarmy.client.aws.AWSClient;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;

/** The basic context for the monkey that tags volumes with Janitor meta data.
 */
public class BasicVolumeTaggingMonkeyContext extends BasicSimianArmyContext implements VolumeTaggingMonkey.Context {

    private final Collection<AWSClient> awsClients = Lists.newArrayList();

    /**
     * The constructor.
     */
    public BasicVolumeTaggingMonkeyContext() {
        super("simianarmy.properties", "client.properties", "volumeTagging.properties");
        for (String r : StringUtils.split(region(), ",")) {
            createClient(r);
            awsClients.add(awsClient());
        }
    }

    @Override
    public Collection<AWSClient> awsClients() {
        return awsClients;
    }
}

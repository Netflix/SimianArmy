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

import com.netflix.simianarmy.Tag;
import java.util.List;


/**
 * The interface that holds the EC2 instance metadata
 */

public interface Instance {

    /**
     * Gets instance id.
     *
     * @return instance id as string
     */
    String getInstanceId();

    /**
     * Gets name.
     *
     * @return name as string
     */
    String getName();

    /**
     * Gets host name.
     *
     * @return host name as string
     */
    String getHostname();

    /**
     * Gets the user created tags for the instance.
     *
     * @return list of tags
     */
    List<Tag> getTags();


}

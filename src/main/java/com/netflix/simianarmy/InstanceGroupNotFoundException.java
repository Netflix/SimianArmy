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
 * The Class InstanceGroupNotFoundException.
 *
 * These exceptions will be thrown when an instance group cannot be found with the
 * given name and type.
 */
public class InstanceGroupNotFoundException extends Exception {

    private static final long serialVersionUID = -5492120875166280476L;

    private final String groupType;
    private final String groupName;

    /**
     * Instantiates an InstanceGroupNotFoundException with the group type and name.
     * @param groupType the group type
     * @param groupName the gruop name
     */
    public InstanceGroupNotFoundException(String groupType, String groupName) {
        super(errorMessage(groupType, groupName));
        this.groupType = groupType;
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return errorMessage(groupType, groupName);
    }

    private static String errorMessage(String groupType, String groupName) {
        return String.format("Instance group named '%s' [type %s] cannot be found.",
                groupName, groupType);
    }
}

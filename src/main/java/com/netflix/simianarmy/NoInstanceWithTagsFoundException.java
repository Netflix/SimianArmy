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
 * The Class NoInstanceWithTagsFoundException.
 *
 * These exceptions will be thrown when an instance with all tags are NOT found
 */
public class NoInstanceWithTagsFoundException extends Exception {
    private static final long serialVersionUID = 01072016L;
    private List<Tag> ec2Tags;

    /**
     * Instantiates an NoInstanceWithTagsFoundException with the tags.
     * @param ec2Tags
     */
    public NoInstanceWithTagsFoundException(List<Tag> ec2Tags) {
        super(errorMessage(ec2Tags));
        this.ec2Tags = ec2Tags;
    }

    @Override
    public String toString() {
        return errorMessage(ec2Tags);
    }

    private static String errorMessage(List<Tag> ec2Tags) {
        StringBuilder error = new StringBuilder(1000);
        error.append(" No Instances with the following tags were found: ");
        for (Tag ec2Tag : ec2Tags) {
            error.append(ec2Tag.getKey() + ":" +  ec2Tag.getValue() + ", ");
        }
        return error.toString();
    }
}
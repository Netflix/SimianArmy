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
 * The Class FeatureNotEnabledException.
 *
 * These exceptions will be thrown when a feature is not enabled when being accessed.
 */
public class FeatureNotEnabledException extends Exception {

    private static final long serialVersionUID = 8392434473284901306L;

    /**
     * Instantiates a FeatureNotEnabledException with a message.
     * @param msg the error message
     */
    public FeatureNotEnabledException(String msg) {
        super(msg);
    }
}

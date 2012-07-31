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
 * The Class NotFoundException.
 *
 * These exceptions will be thrown when a Monkey is trying to interact with a remote resource but it no longer exists
 * (or never existed). It is used as an adapter to translate a cloud provider exception into something common that the
 * monkeys can easily handle.
 */
@SuppressWarnings("serial")
public class NotFoundException extends RuntimeException {

    /**
     * Instantiates a new NotFound exception.
     *
     * @param message
     *            the exception message
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * Instantiates a new NotFound exception.
     *
     * @param message
     *            the exception message
     * @param cause
     *            the exception cause. This should be the raw exception from the cloud provider.
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Instantiates a new NotFound exception.
     *
     * @param cause
     *            the exception cause. This should be the raw exception from the cloud provider.
     */
    public NotFoundException(Throwable cause) {
        super(cause);
    }
}

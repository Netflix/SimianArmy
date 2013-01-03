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


/** The interface for the email notifier used by monkeys. */
public interface MonkeyEmailNotifier {

    /**
     * Determines if a email address is valid.
     * @param email the email
     * @return true if the email address is valid, false otherwise.
     */
    boolean isValidEmail(String email);

    /**
     * Builds an email subject for an email address.
     * @param to the destination email address
     * @return the email subject
     */
    String buildEmailSubject(String to);

    /**
     * Gets the cc email addresses for a to address.
     * @param to the to address
     * @return the cc email addresses
     */
    String[] getCcAddresses(String to);

    /**
     * Gets the source email addresses for a to address.
     * @param to the to address
     * @return the source email addresses
     */
    String getSourceAddress(String to);

    /**
     * Sends an email.
     * @param to the address sent to
     * @param subject the email subject
     * @param body the email body
     */
    void sendEmail(String to, String subject, String body);
}

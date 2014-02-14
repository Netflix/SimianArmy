/*
 *
 *  Copyright 2014 Salesforce.com, Inc.
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
 * The interface for the email client used by monkeys. Generally, the EmailClient is used by a Notifier
 * to use a mail-specific API to construct and deliver the message.
 */
public interface EmailClient {

    /**
     * Determines if a email address is valid.
     * @param email the email
     * @return true if the email address is valid, false otherwise.
     */
    boolean isValidEmail(String email);

    /**
     * Sends an email.
     * @param to the address sent to
     * @param from the address of the sender
     * @param cc addresses to carbon-copy the email to
     * @param subject the email subject
     * @param body the email body
     */
    void sendEmail(String to, String from, String[] cc, String subject, String body);
}

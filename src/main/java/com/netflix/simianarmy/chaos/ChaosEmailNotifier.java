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
package com.netflix.simianarmy.chaos;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.netflix.simianarmy.aws.AWSEmailNotifier;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

/** The email notifier for Chaos monkey.
 *
 */
public abstract class ChaosEmailNotifier extends AWSEmailNotifier {

    /** Constructor. Currently the notifier is fixed the email client to
     * Amazon Simple Email Service. We can release this restriction when
     * we want to support different email clients.
     *
     * @param sesClient the AWS simple email service client.
     */
    public ChaosEmailNotifier(AmazonSimpleEmailServiceClient sesClient) {
        super(sesClient);
    }

    /**
     * Sends an email notification for a termination of instance to group
     * owner's email address.
     * @param group the instance group
     * @param instance the instance id
     * @param chaosType the chosen chaos strategy
     */
    public abstract void sendTerminationNotification(InstanceGroup group, String instance, ChaosType chaosType);

    /**
     * Sends an email notification for a termination of instance to a global
     * email address.
     * @param group the instance group
     * @param instance the instance id
     * @param chaosType the chosen chaos strategy
     */
    public abstract void sendTerminationGlobalNotification(InstanceGroup group, String instance, ChaosType chaosType);

}

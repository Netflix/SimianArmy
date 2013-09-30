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
package com.netflix.simianarmy.basic.chaos;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;
import com.netflix.simianarmy.chaos.ChaosEmailNotifier;
import com.netflix.simianarmy.chaos.ChaosType;

/** The basic implementation of the email notifier for Chaos monkey.
 *
 */
public class BasicChaosEmailNotifier extends ChaosEmailNotifier {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicChaosEmailNotifier.class);

    private final MonkeyConfiguration cfg;
    private final String defaultEmail;
    private final List<String> ccAddresses;

    /** Constructor.
     *
     * @param cfg the monkey configuration
     * @param sesClient the Amazon SES client
     * @param defaultEmail the default email address to notify when the group does not have a
     * owner email specified
     * @param ccAddresses the cc email addresses for notifications
     */
    public BasicChaosEmailNotifier(MonkeyConfiguration cfg, AmazonSimpleEmailServiceClient sesClient,
            String defaultEmail, String... ccAddresses) {
        super(sesClient);
        this.cfg = cfg;
        this.defaultEmail = defaultEmail;
        this.ccAddresses = Arrays.asList(ccAddresses);
    }

    /**
     * Sends an email notification for a termination of instance to a global
     * email address.
     * @param group the instance group
     * @param instanceId the instance id
     * @param chaosType the chosen chaos strategy
     */
    @Override
    public void sendTerminationGlobalNotification(InstanceGroup group, String instanceId, ChaosType chaosType) {
        String to = cfg.getStr("simianarmy.chaos.notification.global.receiverEmail");
        if (StringUtils.isBlank(to)) {
            LOGGER.warn("Global email address was not set, but global email notification was enabled!");
            return;
        }
        LOGGER.info("sending termination notification to global email address {}", to);
        buildAndSendEmail(to, group, instanceId, chaosType);
    }

    /**
     * Sends an email notification for a termination of instance to the group
     * owner's email address.
     * @param group the instance group
     * @param instanceId the instance id
     * @param chaosType the chosen chaos strategy
     */
    @Override
    public void sendTerminationNotification(InstanceGroup group, String instanceId, ChaosType chaosType) {
        String to = getOwnerEmail(group);
        LOGGER.info("sending termination notification to group owner email address {}", to);
        buildAndSendEmail(to, group, instanceId, chaosType);
    }

    /**
     * Gets the owner's email for a instance group.
     * @param group the instance group
     * @return the owner email of the instance group
     */
    protected String getOwnerEmail(InstanceGroup group) {
        String prop = String.format("simianarmy.chaos.%s.%s.ownerEmail", group.type(), group.name());
        String ownerEmail = cfg.getStr(prop);
        if (ownerEmail == null) {
            LOGGER.info(String.format("Property %s is not set, use the default email address %s as"
                    + " the owner email of group %s of type %s",
                    prop, defaultEmail, group.name(), group.type()));
            return defaultEmail;
        } else {
            return ownerEmail;
        }
    }

    /**
     * Builds the body and subject for the email, sends the email.
     * @param group
     *          the instance group
     * @param instanceId
     *          the instance id
     * @param to
     *          the email address to be sent to
     * @param chaosType the chosen chaos strategy
     */
    public void buildAndSendEmail(String to, InstanceGroup group, String instanceId, ChaosType chaosType) {
        String body = buildEmailBody(group, instanceId, chaosType);

        String subject;
        boolean emailSubjectIsBody = cfg.getBoolOrElse(
                "simianarmy.chaos.notification.subject.isBody", false);
        if (emailSubjectIsBody) {
            subject = body;
        } else {
            subject = buildEmailSubject(to);
        }

        sendEmail(to, subject, body);
    }

    @Override
    public String buildEmailSubject(String to) {
        String emailSubjectPrefix = cfg.getStrOrElse("simianarmy.chaos.notification.subject.prefix", "");
        String emailSubjectSuffix = cfg.getStrOrElse("simianarmy.chaos.notification.subject.suffix", "");
        return String.format("%sChaos Monkey Termination Notification for %s%s",
                                                emailSubjectPrefix, to, emailSubjectSuffix);
    }

    /**
     * Builds the body for the email.
     * @param group
     *          the instance group
     * @param instanceId
     *          the instance id
     * @param chaosType the chosen chaos strategy
     * @return the created string
     */
    public String buildEmailBody(InstanceGroup group, String instanceId, ChaosType chaosType) {
        String emailBodyPrefix = cfg.getStrOrElse("simianarmy.chaos.notification.body.prefix", "");
        String emailBodySuffix = cfg.getStrOrElse("simianarmy.chaos.notification.body.suffix", "");
        String body = emailBodyPrefix;
        body += String.format("Instance %s of %s %s is being terminated by Chaos monkey.",
                    instanceId, group.type(), group.name());
        if (chaosType != null) {
            body += "\n";
            body += String.format("Chaos type: %s.", chaosType.getKey());
        }
        body += emailBodySuffix;
        return body;
    }

    @Override
    public String[] getCcAddresses(String to) {
        return ccAddresses.toArray(new String[ccAddresses.size()]);
    }

    @Override
    public String getSourceAddress(String to) {
        String prop = "simianarmy.chaos.notification.sourceEmail";
        String sourceEmail = cfg.getStr(prop);
        if (sourceEmail == null || !isValidEmail(sourceEmail)) {
            String msg = String.format("Property %s is not set or its value is not a valid email.", prop);
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        return sourceEmail;
    }
}
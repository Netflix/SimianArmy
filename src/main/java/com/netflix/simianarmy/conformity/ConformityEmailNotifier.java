/*
 *
 *  Copyright 2013 Netflix, Inc.
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
package com.netflix.simianarmy.conformity;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netflix.simianarmy.aws.AWSEmailNotifier;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The email notifier implemented for Janitor Monkey.
 */
public class ConformityEmailNotifier  extends AWSEmailNotifier {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ConformityEmailNotifier.class);
    private static final String UNKNOWN_EMAIL = "UNKNOWN";

    private final Collection<String> regions = Lists.newArrayList();
    private final String defaultEmail;
    private final List<String> ccEmails = Lists.newArrayList();
    private final ConformityClusterTracker clusterTracker;
    private final ConformityEmailBuilder emailBuilder;
    private final String sourceEmail;
    private final Map<String, Collection<Cluster>> invalidEmailToClusters = Maps.newHashMap();
    private final Collection<ConformityRule> rules = Lists.newArrayList();
    private final int openHour;
    private final int closeHour;

    /**
     * The Interface Context.
     */
    public interface Context {
        /**
         * Gets the Amazon Simple Email Service client.
         * @return the Amazon Simple Email Service client
         */
        AmazonSimpleEmailServiceClient sesClient();

        /**
         * Gets the open hour the email notifications are sent.
         * @return
         *      the open hour the email notifications are sent
         */
        int openHour();

        /**
         * Gets the close hour the email notifications are sent.
         * @return
         *      the close hour the email notifications are sent
         */
        int closeHour();

        /**
         * Gets the source email the notifier uses to send email.
         * @return the source email
         */
        String sourceEmail();

        /**
         * Gets the default email the notifier sends to when there is no owner specified for a cluster.
         * @return the default email
         */
        String defaultEmail();

        /**
         * Gets the regions the notifier is running in.
         * @return the regions the notifier is running in.
         */
        Collection<String> regions();

        /** Gets the Conformity Monkey's cluster tracker.
         * @return the Conformity Monkey's cluster tracker
         */
        ConformityClusterTracker clusterTracker();

        /** Gets the Conformity email builder.
         * @return the Conformity email builder
         */
        ConformityEmailBuilder emailBuilder();

        /** Gets the cc email addresses.
         * @return the cc email addresses
         */
        String[] ccEmails();

        /**
         * Gets all the conformity rules.
         * @return all conformity rules.
         */
        Collection<ConformityRule> rules();
    }

    /**
     * Constructor.
     * @param ctx the context.
     */
    public ConformityEmailNotifier(Context ctx) {
        super(ctx.sesClient());
        this.openHour = ctx.openHour();
        this.closeHour = ctx.closeHour();
        for (String region : ctx.regions()) {
            this.regions.add(region);
        }
        this.defaultEmail = ctx.defaultEmail();
        this.clusterTracker = ctx.clusterTracker();
        this.emailBuilder = ctx.emailBuilder();
        String[] ctxCCs = ctx.ccEmails();
        if (ctxCCs != null) {
            for (String ccEmail : ctxCCs) {
                this.ccEmails.add(ccEmail);
            }
        }
        this.sourceEmail = ctx.sourceEmail();
        Validate.notNull(ctx.rules());
        for (ConformityRule rule : ctx.rules()) {
            rules.add(rule);
        }
    }

    /**
     * Gets all the clusters that are not conforming and sends email notifications to the owners.
     */
    public void sendNotifications() {
        int currentHour = DateTime.now().getHourOfDay();
        if (currentHour < openHour || currentHour > closeHour) {
            LOGGER.info("It is not the time for Conformity Monkey to send notifications. You can change "
                    + "simianarmy.conformity.notification.openHour and simianarmy.conformity.notification.openHour"
                    + " to make it work at this hour.");
            return;
        }

        validateEmails();
        Map<String, Collection<Cluster>> emailToClusters = Maps.newHashMap();
        for (Cluster cluster : clusterTracker.getNonconformingClusters(regions.toArray(new String[regions.size()]))) {
            if (cluster.isOptOutOfConformity()) {
                LOGGER.info(String.format("Cluster %s is opted out of Conformity Monkey so no notification is sent.",
                        cluster.getName()));
                continue;
            }
            if (!cluster.isConforming()) {
                String email = cluster.getOwnerEmail();
                if (!isValidEmail(email)) {
                    if (defaultEmail != null) {
                        LOGGER.info(String.format("Email %s is not valid, send to the default email address %s",
                                email, defaultEmail));
                        putEmailAndCluster(emailToClusters, defaultEmail, cluster);
                    } else {
                        if (email == null) {
                            email = UNKNOWN_EMAIL;
                        }
                        LOGGER.info(String.format("Email %s is not valid and default email is not set for cluster %s",
                                email, cluster.getName()));
                        putEmailAndCluster(invalidEmailToClusters, email, cluster);
                    }
                } else {
                    putEmailAndCluster(emailToClusters, email, cluster);
                }
            } else {
                LOGGER.debug(String.format("Cluster %s is conforming so no notification needs to be sent.",
                        cluster.getName()));
            }
        }
        emailBuilder.setEmailToClusters(emailToClusters, rules);
        for (Map.Entry<String, Collection<Cluster>> entry : emailToClusters.entrySet()) {
            String email = entry.getKey();
            String emailBody = emailBuilder.buildEmailBody(email);
            String subject = buildEmailSubject(email);
            sendEmail(email, subject, emailBody);
            for (Cluster cluster : entry.getValue()) {
                LOGGER.debug(String.format("Notification is sent for cluster %s to %s", cluster.getName(), email));
            }
            LOGGER.info(String.format("Email notification has been sent to %s for %d clusters.",
                    email, entry.getValue().size()));
        }
    }


    @Override
    public String buildEmailSubject(String to) {
        return String.format("Conformity Monkey Notification for %s", to);
    }

    @Override
    public String[] getCcAddresses(String to) {
        return ccEmails.toArray(new String[ccEmails.size()]);
    }

    @Override
    public String getSourceAddress(String to) {
        return sourceEmail;
    }

    private void validateEmails() {
        if (defaultEmail != null) {
            Validate.isTrue(isValidEmail(defaultEmail), String.format("Default email %s is invalid", defaultEmail));
        }
        if (ccEmails != null) {
            for (String ccEmail : ccEmails) {
                Validate.isTrue(isValidEmail(ccEmail), String.format("CC email %s is invalid", ccEmail));
            }
        }
    }

    private void putEmailAndCluster(Map<String, Collection<Cluster>> map, String email, Cluster cluster) {
        Collection<Cluster> clusters = map.get(email);
        if (clusters == null) {
            clusters = Lists.newArrayList();
            map.put(email, clusters);
        }
        clusters.add(cluster);
    }
}

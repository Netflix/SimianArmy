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
package com.netflix.simianarmy.janitor;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.Resource.CleanupState;
import com.netflix.simianarmy.aws.AWSEmailNotifier;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The email notifier implemented for Janitor Monkey. */
public class JanitorEmailNotifier extends AWSEmailNotifier {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JanitorEmailNotifier.class);
    private static final String UNKNOWN_EMAIL = "UNKNOWN";
    /**
     * If the scheduled termination date is within 2 hours of notification date + headsup days,
     * we don't need to extend the termination date.
     */
    private static final int HOURS_IN_MARGIN = 2;

    private final String region;
    private final String defaultEmail;
    private final List<String> ccEmails;
    private final JanitorResourceTracker resourceTracker;
    private final JanitorEmailBuilder emailBuilder;
    private final MonkeyCalendar calendar;
    private final int daysBeforeTermination;
    private final String sourceEmail;
    private final String ownerEmailDomain;
    private final Map<String, Collection<Resource>> invalidEmailToResources =
            new HashMap<String, Collection<Resource>>();

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
         * Gets the source email the notifier uses to send email.
         * @return the source email
         */
        String sourceEmail();

        /**
         * Gets the default email the notifier sends to when there is no owner specified for a resource.
         * @return the default email
         */
        String defaultEmail();

        /**
         * Gets the number of days a notification is sent before the expected termination date..
         * @return the number of days a notification is sent before the expected termination date.
         */
        int daysBeforeTermination();

        /**
         * Gets the region the notifier is running in.
         * @return the region the notifier is running in.
         */
        String region();

        /** Gets the janitor resource tracker.
         * @return the janitor resource tracker
         */
        JanitorResourceTracker resourceTracker();

        /** Gets the janitor email builder.
         * @return the janitor email builder
         */
        JanitorEmailBuilder emailBuilder();

        /** Gets the calendar.
         * @return the calendar
         */
        MonkeyCalendar calendar();

        /** Gets the cc email addresses.
         * @return the cc email addresses
         */
        String[] ccEmails();

        /** Get the default domain of email addresses.
         * @return the default domain of email addresses
         */
        String ownerEmailDomain();
    }

    /**
     * Constructor.
     * @param ctx the context.
     */
    public JanitorEmailNotifier(Context ctx) {
        super(ctx.sesClient());
        this.region = ctx.region();
        this.defaultEmail = ctx.defaultEmail();
        this.daysBeforeTermination = ctx.daysBeforeTermination();
        this.resourceTracker = ctx.resourceTracker();
        this.emailBuilder = ctx.emailBuilder();
        this.calendar = ctx.calendar();
        this.ccEmails = new ArrayList<String>();
        String[] ctxCCs = ctx.ccEmails();
        if (ctxCCs != null) {
            for (String ccEmail : ctxCCs) {
                this.ccEmails.add(ccEmail);
            }
        }
        this.sourceEmail = ctx.sourceEmail();
        this.ownerEmailDomain = ctx.ownerEmailDomain();
    }

    /**
     * Gets all the resources that are marked and no notifications have been sent. Send email notifications
     * for these resources. If there is a valid email address in the ownerEmail field of the resource, send
     * to that address. Otherwise send to the default email address.
     */
    public void sendNotifications() {
        validateEmails();
        Map<String, Collection<Resource>> emailToResources = new HashMap<String, Collection<Resource>>();
        invalidEmailToResources.clear();
        for (Resource r : getMarkedResources()) {
            if (r.isOptOutOfJanitor()) {
                LOGGER.info(String.format("Resource %s is opted out of Janitor Monkey so no notification is sent.",
                        r.getId()));
                continue;
            }
            if (canNotify(r)) {
                String email = r.getOwnerEmail();
                if (email != null && !email.contains("@")
                      && StringUtils.isNotBlank(this.ownerEmailDomain)) {
                    email = String.format("%s@%s", email, this.ownerEmailDomain);
                }
                if (!isValidEmail(email)) {
                    if (defaultEmail != null) {
                        LOGGER.info(String.format("Email %s is not valid, send to the default email address %s",
                                email, defaultEmail));
                        putEmailAndResource(emailToResources, defaultEmail, r);
                    } else {
                        if (email == null) {
                            email = UNKNOWN_EMAIL;
                        }
                        LOGGER.info(String.format("Email %s is not valid and default email is not set for resource %s",
                                email, r.getId()));
                        putEmailAndResource(invalidEmailToResources, email, r);
                    }
                } else {
                    putEmailAndResource(emailToResources, email, r);
                }
            } else {
                LOGGER.debug(String.format("Not the time to send notification for resource %s", r.getId()));
            }
        }
        emailBuilder.setEmailToResources(emailToResources);
        Date now = calendar.now().getTime();
        for (Map.Entry<String, Collection<Resource>> entry : emailToResources.entrySet()) {
            String email = entry.getKey();
            String emailBody = emailBuilder.buildEmailBody(email);
            String subject = buildEmailSubject(email);
            sendEmail(email, subject, emailBody);
            for (Resource r : entry.getValue()) {
                LOGGER.debug(String.format("Notification is sent for resource %s", r.getId()));
                r.setNotificationTime(now);
                resourceTracker.addOrUpdate(r);
            }
            LOGGER.info(String.format("Email notification has been sent to %s for %d resources.",
                    email, entry.getValue().size()));
        }
    }

    /**
     * Gets the marked resources for notification. Allow overriding in subclasses.
     * @return the marked resources
     */
    protected Collection<Resource> getMarkedResources() {
        return resourceTracker.getResources(null, CleanupState.MARKED, region);
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

    @Override
    public String buildEmailSubject(String email) {
        return String.format("Janitor Monkey Notification for %s", email);
    }

    /**
     * Decides if it is time for sending notification for the resource. This method can be
     * overridden in subclasses so notifications can be send earlier or later.
     * @param resource the resource
     * @return true if it is OK to send notification now, otherwise false.
     */
    protected boolean canNotify(Resource resource) {
        Validate.notNull(resource);
        if (resource.getState() != CleanupState.MARKED || resource.isOptOutOfJanitor()) {
            return false;
        }

        Date notificationTime = resource.getNotificationTime();
        // We don't want to send notification too early (since things may change) or too late (we need
        // to give owners enough time to take actions.
        Date windowStart = new Date(new DateTime(
                calendar.getBusinessDay(calendar.now().getTime(), daysBeforeTermination).getTime())
                .minusHours(HOURS_IN_MARGIN).getMillis());
        Date windowEnd = calendar.getBusinessDay(calendar.now().getTime(), daysBeforeTermination + 1);
        Date terminationDate = resource.getExpectedTerminationTime();
        if (notificationTime == null
                || notificationTime.getTime() == 0
                || resource.getMarkTime().after(notificationTime)) { // remarked after a notification
            if (!terminationDate.before(windowStart) && !terminationDate.after(windowEnd)) {
                // The expected termination time is close enough for sending notification
                return true;
            } else if (terminationDate.before(windowStart)) {
                // The expected termination date is too close. To give the owner time to take possible actions,
                // we extend the expected termination time here.
                LOGGER.info(String.format("It is less than %d days before the expected termination date,"
                        + " of resource %s, extending the termination time to %s.",
                        daysBeforeTermination, resource.getId(), windowStart));
                resource.setExpectedTerminationTime(windowStart);
                resourceTracker.addOrUpdate(resource);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * Gets the map from invalid email address to the resources that were supposed to be sent to the address.
     *
     * @return the map from invalid address to resources that failed to be delivered
     */
    public Map<String, Collection<Resource>> getInvalidEmailToResources() {
        return Collections.unmodifiableMap(invalidEmailToResources);
    }

    @Override
    public String[] getCcAddresses(String to) {
        return ccEmails.toArray(new String[ccEmails.size()]);
    }

    @Override
    public String getSourceAddress(String to) {
        return sourceEmail;
    }

    private void putEmailAndResource(
            Map<String, Collection<Resource>> map, String email, Resource resource) {
        Collection<Resource> resources = map.get(email);
        if (resources == null) {
            resources = new ArrayList<Resource>();
            map.put(email, resources);
        }
        resources.add(resource);
    }
}

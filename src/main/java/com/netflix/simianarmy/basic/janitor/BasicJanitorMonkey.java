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
package com.netflix.simianarmy.basic.janitor;

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.janitor.AbstractJanitor;
import com.netflix.simianarmy.janitor.JanitorEmailNotifier;
import com.netflix.simianarmy.janitor.JanitorMonkey;
import com.netflix.simianarmy.janitor.JanitorResourceTracker;

/** The basic implementation of Janitor Monkey. */
public class BasicJanitorMonkey extends JanitorMonkey {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicJanitorMonkey.class);

    /** The Constant NS. */
    private static final String NS = "simianarmy.janitor.";

    /** The cfg. */
    private final MonkeyConfiguration cfg;

    private final List<AbstractJanitor> janitors;

    private final JanitorEmailNotifier emailNotifier;

    private final String region;

    private final JanitorResourceTracker resourceTracker;

    private final MonkeyRecorder recorder;

    private final MonkeyCalendar calendar;

    /**
     * Instantiates a new basic janitor monkey.
     *
     * @param ctx
     *            the ctx
     */
    public BasicJanitorMonkey(Context ctx) {
        super(ctx);
        this.cfg = ctx.configuration();

        janitors = ctx.janitors();
        emailNotifier = ctx.emailNotifier();
        region = ctx.region();
        resourceTracker = ctx.resourceTracker();
        recorder = ctx.recorder();
        calendar = ctx.calendar();
    }

    /** {@inheritDoc} */
    @Override
    public void doMonkeyBusiness() {
        cfg.reload();
        context().resetEventReport();

        if (!isJanitorMonkeyEnabled()) {
            return;
        } else {
            LOGGER.info(String.format("Marking resources with %d janitors.", janitors.size()));
            for (AbstractJanitor janitor : janitors) {
                LOGGER.info(String.format("Running janitor for region %s", janitor.getRegion()));
                janitor.markResources();
                LOGGER.info(String.format("Marked %d resources of type %s in the last run.",
                        janitor.getMarkedResources().size(), janitor.getResourceType().name()));
                LOGGER.info(String.format("Unmarked %d resources of type %s in the last run.",
                        janitor.getUnmarkedResources().size(), janitor.getResourceType()));
            }

            if (!cfg.getBoolOrElse("simianarmy.janitor.leashed", true)) {
                emailNotifier.sendNotifications();
            } else {
                LOGGER.info("Janitor Monkey is leashed, no notification is sent.");
            }

            LOGGER.info(String.format("Cleaning resources with %d janitors.", janitors.size()));
            for (AbstractJanitor janitor : janitors) {
                janitor.cleanupResources();
                LOGGER.info(String.format("Cleaned %d resources of type %s in the last run.",
                        janitor.getCleanedResources().size(), janitor.getResourceType()));
                LOGGER.info(String.format("Failed to clean %d resources of type %s in the last run.",
                        janitor.getFailedToCleanResources().size(), janitor.getResourceType()));
            }
            if (cfg.getBoolOrElse(NS + "summaryEmail.enabled", true)) {
                sendJanitorSummaryEmail();
            }
        }
    }

    @Override
    public Event optInResource(String resourceId) {
        return optInOrOutResource(resourceId, true);
    }

    @Override
    public Event optOutResource(String resourceId) {
        return optInOrOutResource(resourceId, false);
    }

    private Event optInOrOutResource(String resourceId, boolean optIn) {
        Resource resource = resourceTracker.getResource(resourceId);
        if (resource == null) {
            return null;
        }
        EventTypes eventType = optIn ? EventTypes.OPT_IN_RESOURCE : EventTypes.OPT_OUT_RESOURCE;
        long timestamp = calendar.now().getTimeInMillis();
        // The same resource can have multiple events, so we add the timestamp to the id.
        Event evt = recorder.newEvent(Type.JANITOR, eventType, region, resourceId + "@" + timestamp);
        recorder.recordEvent(evt);
        resource.setOptOutOfJanitor(!optIn);
        resourceTracker.addOrUpdate(resource);
        return evt;
    }

    /**
     * Send a summary email with about the last run of the janitor monkey.
     */
    protected void sendJanitorSummaryEmail() {
        String summaryEmailTarget = cfg.getStr(NS + "summaryEmail.to");
        if (!StringUtils.isEmpty(summaryEmailTarget)) {
            if (!emailNotifier.isValidEmail(summaryEmailTarget)) {
                LOGGER.error(String.format("The email target address '%s' for Janitor summary email is invalid",
                        summaryEmailTarget));
                return;
            }
            StringBuilder message = new StringBuilder();
            for (AbstractJanitor janitor : janitors) {
                ResourceType resourceType = janitor.getResourceType();
                appendSummary(message, "markings", resourceType, janitor.getMarkedResources(), janitor.getRegion());
                appendSummary(message, "unmarkings", resourceType, janitor.getUnmarkedResources(), janitor.getRegion());
                appendSummary(message, "cleanups", resourceType, janitor.getCleanedResources(), janitor.getRegion());
                appendSummary(message, "cleanup failures", resourceType, janitor.getFailedToCleanResources(),
                        janitor.getRegion());
            }
            String subject = getSummaryEmailSubject();
            emailNotifier.sendEmail(summaryEmailTarget, subject, message.toString());
        }
    }

    private void appendSummary(StringBuilder message, String summaryName,
            ResourceType resourceType, Collection<Resource> resources, String janitorRegion) {
        message.append(String.format("Total %s for %s = %d in region %s<br/>",
                summaryName, resourceType.name(), resources.size(), janitorRegion));
        message.append(String.format("List: %s<br/>", printResources(resources)));
    }

    private String printResources(Collection<Resource> resources) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Resource r : resources) {
            if (!isFirst) {
                sb.append(",");
            } else {
                isFirst = false;
            }
            sb.append(r.getId());
        }
        return sb.toString();
    }

    /**
     * Gets the summary email subject for the last run of janitor monkey.
     * @return the subject of the summary email
     */
    protected String getSummaryEmailSubject() {
        return String.format("Janitor monkey execution summary (%s)", region);
    }

    /**
     * Handle cleanup error. This has been abstracted so subclasses can decide to continue causing chaos if desired.
     *
     * @param resource
     *            the instance
     * @param e
     *            the exception
     */
    protected void handleCleanupError(Resource resource, Throwable e) {
        String msg = String.format("Failed to clean up %s resource %s with error %s",
                resource.getResourceType(), resource.getId(), e.getMessage());
        LOGGER.error(msg);
        throw new RuntimeException(msg, e);
    }

    private boolean isJanitorMonkeyEnabled() {
        String prop = NS + "enabled";
        if (cfg.getBoolOrElse(prop, true)) {
            return true;
        }
        LOGGER.info("JanitorMonkey disabled, set {}=true", prop);
        return false;
    }
}

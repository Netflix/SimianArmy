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
import java.util.concurrent.atomic.AtomicLong;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;
import com.netflix.simianarmy.*;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.janitor.AbstractJanitor;
import com.netflix.simianarmy.janitor.JanitorEmailNotifier;
import com.netflix.simianarmy.janitor.JanitorMonkey;
import com.netflix.simianarmy.janitor.JanitorResourceTracker;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final String accountName;

    private final JanitorResourceTracker resourceTracker;

    private final MonkeyRecorder recorder;

    private final MonkeyCalendar calendar;
    
    /** Keep track of the number of monkey runs */
    protected final AtomicLong monkeyRuns = new AtomicLong(0);    

    /** Keep track of the number of monkey errors */
    protected final AtomicLong monkeyErrors = new AtomicLong(0);    
    
    /** Emit a servor signal to track the running monkey */
    protected final AtomicLong monkeyRunning = new AtomicLong(0);    
    
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
        accountName = ctx.accountName();
        resourceTracker = ctx.resourceTracker();
        recorder = ctx.recorder();
        calendar = ctx.calendar();

        // register this janitor with servo
        Monitors.registerObject("simianarmy.janitor", this);                
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
            monkeyRuns.incrementAndGet();
            monkeyRunning.set(1);
            
            // prepare to run, this just resets the counts so monitoring is sane
            for (AbstractJanitor janitor : janitors) {
            	janitor.prepareToRun();
            }
            
            for (AbstractJanitor janitor : janitors) {
                LOGGER.info(String.format("Running %s janitor for region %s", janitor.getResourceType(), janitor.getRegion()));
                try {
                	janitor.markResources();
                } catch (Exception e) {
                	monkeyErrors.incrementAndGet();
                	LOGGER.error(String.format("Got an exception while %s janitor was marking for region %s", janitor.getResourceType(), janitor.getRegion()), e);
                }
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
            	try {
            		janitor.cleanupResources();
                } catch (Exception e) {
                	monkeyErrors.incrementAndGet();
                	LOGGER.error(String.format("Got an exception while %s janitor was cleaning for region %s", janitor.getResourceType(), janitor.getRegion()), e);
                }
                LOGGER.info(String.format("Cleaned %d resources of type %s in the last run.",
                        janitor.getCleanedResources().size(), janitor.getResourceType()));
                LOGGER.info(String.format("Failed to clean %d resources of type %s in the last run.",
                        janitor.getFailedToCleanResources().size(), janitor.getResourceType()));
            }
            if (cfg.getBoolOrElse(NS + "summaryEmail.enabled", true)) {
                sendJanitorSummaryEmail();
            }
        	monkeyRunning.set(0);
        }
    }

    @Override
    public Event optInResource(String resourceId) {
        return optInOrOutResource(resourceId, true, region);
    }

    @Override
    public Event optOutResource(String resourceId) {
        return optInOrOutResource(resourceId, false, region);
    }

    @Override
    public Event optInResource(String resourceId, String resourceRegion) {
        return optInOrOutResource(resourceId, true, resourceRegion);
    }

    @Override
    public Event optOutResource(String resourceId, String resourceRegion) {
        return optInOrOutResource(resourceId, false, resourceRegion);
    }

    private Event optInOrOutResource(String resourceId, boolean optIn, String resourceRegion) {
        if (resourceRegion == null) {
            resourceRegion = region;
        }

        Resource resource = resourceTracker.getResource(resourceId, resourceRegion);
        if (resource == null) {
            return null;
        }

        EventTypes eventType = optIn ? EventTypes.OPT_IN_RESOURCE : EventTypes.OPT_OUT_RESOURCE;
        long timestamp = calendar.now().getTimeInMillis();
        // The same resource can have multiple events, so we add the timestamp to the id.
        Event evt = recorder.newEvent(Type.JANITOR, eventType, resource, resourceId + "@" + timestamp);
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
        return String.format("Janitor monkey execution summary (%s, %s)", accountName, region);
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
    
    @Monitor(name="runs", type=DataSourceType.COUNTER)
    public long getMonkeyRuns() {
      return monkeyRuns.get();
    }

    @Monitor(name="errors", type=DataSourceType.GAUGE)
    public long getMonkeyErrors() {
      return monkeyErrors.get();
    }

    @Monitor(name="running", type=DataSourceType.GAUGE)
    public long getMonkeyRunning() {
      return monkeyRunning.get();
    }
    
}

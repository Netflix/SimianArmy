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

import com.google.common.collect.Maps;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.MonkeyRecorder.Event;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.Resource.CleanupState;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.janitor.JanitorMonkey.EventTypes;
import com.netflix.simianarmy.janitor.JanitorMonkey.Type;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An abstract implementation of Janitor. It marks resources that the rule engine considers
 * invalid as cleanup candidate and sets the expected termination date. It also removes the
 * cleanup candidate flag from resources that no longer exist or the rule engine no longer
 * considers invalid due to change of conditions. For resources marked as cleanup candidates
 * and the expected termination date is passed, the janitor removes the resources from the
 * cloud.
 */
public abstract class AbstractJanitor implements Janitor {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJanitor.class);

    private final String region;
    /** The region the janitor is running in. */
    public String getRegion() {
        return region;
    }

    /**
     * The rule engine used to decide if a resource should be a cleanup
     * candidate.
     */
    private final JanitorRuleEngine ruleEngine;

    /** The janitor crawler to get resources from the cloud. */
    private final JanitorCrawler crawler;

    /** The resource type that the janitor is responsible for to clean up. **/
    private final ResourceType resourceType;

    /** The janitor resource tracker that is responsible for keeping track of
     * resource status.
     */
    private final JanitorResourceTracker resourceTracker;

    private final Collection<Resource> markedResources = new ArrayList<Resource>();

    private final Collection<Resource> cleanedResources = new ArrayList<Resource>();

    private final Collection<Resource> unmarkedResources = new ArrayList<Resource>();

    private final Collection<Resource> failedToCleanResources = new ArrayList<Resource>();

    private final MonkeyCalendar calendar;

    private final MonkeyConfiguration config;

    /** Flag to indicate whether the Janitor is leashed. */
    private boolean leashed;

    private final MonkeyRecorder recorder;

    /**
     * Sets the flag to indicate if the janitor is leashed.
     *
     * @param isLeashed true if the the janitor is leased, false otherwise.
     */
    protected void setLeashed(boolean isLeashed) {
        this.leashed = isLeashed;
    }

    /**
     * The Interface Context.
     */
    public interface Context {

        /** Region.
         *
         * @return the region
         */
        String region();

        /**
         * Configuration.
         *
         * @return the monkey configuration
         */
        MonkeyConfiguration configuration();

        /**
         * Calendar.
         *
         * @return the monkey calendar
         */
        MonkeyCalendar calendar();

        /**
         * Janitor rule engine.
         * @return the janitor rule engine
         */
        JanitorRuleEngine janitorRuleEngine();

        /**
         * Janitor crawler.
         *
         * @return the chaos crawler
         */
        JanitorCrawler janitorCrawler();

        /**
         * Janitor resource tracker.
         *
         * @return the janitor resource tracker
         */
        JanitorResourceTracker janitorResourceTracker();

        /**
         * Recorder.
         *
         * @return the recorder to record events
         */
        MonkeyRecorder recorder();
    }

    /**
     * Constructor.
     * @param ctx the context
     * @param resourceType the resource type the janitor is taking care
     */
    public AbstractJanitor(Context ctx, ResourceType resourceType) {
        Validate.notNull(ctx);
        Validate.notNull(resourceType);
        this.region = ctx.region();
        Validate.notNull(region);
        this.ruleEngine = ctx.janitorRuleEngine();
        Validate.notNull(ruleEngine);
        this.crawler = ctx.janitorCrawler();
        Validate.notNull(crawler);
        this.resourceTracker = ctx.janitorResourceTracker();
        Validate.notNull(resourceTracker);
        this.calendar = ctx.calendar();
        Validate.notNull(calendar);
        this.config = ctx.configuration();
        Validate.notNull(config);
        // By default the janitor is leashed.
        this.leashed = config.getBoolOrElse("simianarmy.janitor.leashed", true);
        this.resourceType = resourceType;
        Validate.notNull(resourceType);
        // recorder could be null and no events are recorded when it is.
        this.recorder = ctx.recorder();
    }

    @Override
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * Marks all resources obtained from the crawler as cleanup candidate if
     * the janitor rule engine thinks so.
     */
    @Override
    public void markResources() {
        markedResources.clear();
        unmarkedResources.clear();
        Map<String, Resource> trackedMarkedResources = getTrackedMarkedResources();

        List<Resource> crawledResources = crawler.resources(resourceType);
        LOGGER.info(String.format("Looking for cleanup candidate in %d crawled resources.",
                crawledResources.size()));
        Date now = calendar.now().getTime();
        for (Resource resource : crawledResources) {
            Resource trackedResource = trackedMarkedResources.get(resource.getId());
            if (!ruleEngine.isValid(resource)) {
                // If the resource is already marked, ignore it
                if (trackedResource != null) {
                    LOGGER.debug(String.format("Resource %s is already marked.", resource.getId()));
                    continue;
                }
                LOGGER.info(String.format("Marking resource %s of type %s with expected termination time as %s",
                        resource.getId(), resource.getResourceType(), resource.getExpectedTerminationTime()));
                resource.setState(CleanupState.MARKED);
                resource.setMarkTime(now);
                if (!leashed) {
                    if (recorder != null) {
                        Event evt = recorder.newEvent(Type.JANITOR, EventTypes.MARK_RESOURCE, region, resource.getId());
                        recorder.recordEvent(evt);
                    }
                    resourceTracker.addOrUpdate(resource);
                    postMark(resource);
                } else {
                    LOGGER.info(String.format(
                            "The janitor is leashed, no data change is made for marking the resource %s.",
                            resource.getId()));
                }
                markedResources.add(resource);
            } else if (trackedResource != null) {
                // The resource was marked and now the rule engine does not consider it as a cleanup candidate.
                // So the janitor needs to unmark the resource.
                LOGGER.info(String.format("Unmarking resource %s", resource.getId()));
                resource.setState(CleanupState.UNMARKED);
                if (!leashed) {
                    if (recorder != null) {
                        Event evt = recorder.newEvent(
                                Type.JANITOR, EventTypes.UNMARK_RESOURCE, region, resource.getId());
                        recorder.recordEvent(evt);
                    }
                    resourceTracker.addOrUpdate(resource);
                } else {
                    LOGGER.info(String.format(
                            "The janitor is leashed, no data change is made for unmarking the resource %s.",
                            resource.getId()));
                }
                unmarkedResources.add(resource);
            }
        }

        // Unmark the resources that are terminated by user so not returned by the crawler.
        unmarkUserTerminatedResources(crawledResources, trackedMarkedResources);
    }


    /**
     * Gets the existing resources that are marked as cleanup candidate. Allowing the subclass to override for e.g.
     * to handle multi-region.
     * @return the map from resource id to marked resource
     */
    protected Map<String, Resource> getTrackedMarkedResources() {
        Map<String, Resource> trackedMarkedResources = Maps.newHashMap();
        for (Resource resource : resourceTracker.getResources(resourceType, Resource.CleanupState.MARKED, region)) {
            trackedMarkedResources.put(resource.getId(), resource);
        }
        return trackedMarkedResources;
    }

    /**
     * Cleans up all cleanup candidates that are OK to remove.
     */
    @Override
    public void cleanupResources() {
        cleanedResources.clear();
        failedToCleanResources.clear();
        Map<String, Resource> trackedMarkedResources = getTrackedMarkedResources();
        LOGGER.info(String.format("Checking %d marked resources for cleanup.", trackedMarkedResources.size()));

        Date now = calendar.now().getTime();
        for (Resource markedResource : trackedMarkedResources.values()) {
            if (canClean(markedResource, now)) {
                LOGGER.info(String.format("Cleaning up resource %s of type %s",
                        markedResource.getId(), markedResource.getResourceType().name()));
                if (!leashed) {
                    try {
                        if (recorder != null) {
                            Event evt = recorder.newEvent(Type.JANITOR, EventTypes.CLEANUP_RESOURCE, region,
                                    markedResource.getId());
                            recorder.recordEvent(evt);
                        }
                        cleanup(markedResource);
                        markedResource.setActualTerminationTime(now);
                        markedResource.setState(Resource.CleanupState.JANITOR_TERMINATED);
                        resourceTracker.addOrUpdate(markedResource);
                    } catch (Exception e) {
                        LOGGER.error(String.format("Failed to clean up the resource %s.",
                                markedResource.getId()), e);
                        failedToCleanResources.add(markedResource);
                        continue;
                    }
                    postCleanup(markedResource);
                } else {
                    LOGGER.info(String.format(
                            "The janitor is leashed, no data change is made for cleaning up the resource %s.",
                            markedResource.getId()));
                }
                cleanedResources.add(markedResource);
            }
        }
    }

    /** Determines if the input resource can be cleaned. The Janitor calls this method
     * before cleaning up a resource and only cleans the resource when the method returns
     * true. A resource is considered to be OK to clean if
     * 1) it is marked as cleanup candidates
     * 2) the expected termination time is already passed
     * 3) the owner has already been notified about the cleanup
     * 4) the resource is not opted out of Janitor monkey
     * The method can be overridden in subclasses.
     * @param resource the resource the Janitor considers to clean
     * @param now the time that represents the current time
     * @return true if the resource is OK to clean, false otherwise
     */
    protected boolean canClean(Resource resource, Date now) {
        return resource.getState() == Resource.CleanupState.MARKED
                && !resource.isOptOutOfJanitor()
                && resource.getExpectedTerminationTime() != null
                && resource.getExpectedTerminationTime().before(now)
                && resource.getNotificationTime() != null
                && resource.getNotificationTime().before(now);
    }

    /**
     * Implements required operations after a resource is marked.
     * @param resource The resource that is marked
     */
    protected abstract void postMark(Resource resource);

    /**
     * Cleans a resource up, e.g. deleting the resource from the cloud.
     * @param resource The resource that is cleaned up.
     */
    protected abstract void cleanup(Resource resource);

    /**
     * Implements required operations after a resource is cleaned.
     * @param resource The resource that is cleaned up.
     */
    protected abstract void postCleanup(Resource resource);

    /** gets the resources marked in the last run of the Janitor. */
    public Collection<Resource> getMarkedResources() {
        return Collections.unmodifiableCollection(markedResources);
    }

    /** gets the resources unmarked in the last run of the Janitor. */
    public Collection<Resource> getUnmarkedResources() {
        return Collections.unmodifiableCollection(unmarkedResources);
    }

    /** gets the resources cleaned in the last run of the Janitor. */
    public Collection<Resource> getCleanedResources() {
        return Collections.unmodifiableCollection(cleanedResources);
    }

    /** gets the resources that failed to be cleaned in the last run of the Janitor. */
    public Collection<Resource> getFailedToCleanResources() {
        return Collections.unmodifiableCollection(failedToCleanResources);
    }

    private void unmarkUserTerminatedResources(
            List<Resource> crawledResources, Map<String, Resource> trackedMarkedResources) {
        Set<String> crawledResourceIds = new HashSet<String>();
        for (Resource crawledResource : crawledResources) {
            crawledResourceIds.add(crawledResource.getId());
        }
        for (Resource markedResource : trackedMarkedResources.values()) {
            if (!crawledResourceIds.contains(markedResource.getId())) {
                // The resource does not exist anymore.
                LOGGER.info(String.format(
                        "Resource %s is not returned by the crawler. It should already be terminated.",
                        markedResource.getId()));
                if (!leashed) {
                    markedResource.setState(Resource.CleanupState.USER_TERMINATED);
                    resourceTracker.addOrUpdate(markedResource);
                } else {
                    LOGGER.info(String.format(
                            "The janitor is leashed, no data change is made for unmarking "
                                    + "the user terminated resource %s.",
                                    markedResource.getId()));
                }
                unmarkedResources.add(markedResource);
            }
        }
    }
}

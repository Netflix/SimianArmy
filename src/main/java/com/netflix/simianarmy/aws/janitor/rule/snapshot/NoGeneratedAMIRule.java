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
package com.netflix.simianarmy.aws.janitor.rule.snapshot;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.janitor.crawler.EBSSnapshotJanitorCrawler;
import com.netflix.simianarmy.janitor.JanitorMonkey;
import com.netflix.simianarmy.janitor.Rule;

/**
 * The rule is for checking whether an EBS snapshot has any AMIs generated from it.
 * If there are no AMIs generated using the snapshot and the snapshot is created
 * for certain days, it is marked as a cleanup candidate by this rule.
 */
public class NoGeneratedAMIRule implements Rule {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NoGeneratedAMIRule.class);

    private String ownerEmailOverride = null;
    
    private static final String TERMINATION_REASON = "No AMI is generated for this snapshot";

    private final MonkeyCalendar calendar;

    private final int ageThreshold;

    private final int retentionDays;

    /** The date format used to print or parse the user specified termination date. **/
    public static final DateTimeFormatter TERMINATION_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");

    /**
     * Constructor.
     *
     * @param calendar
     *            The calendar used to calculate the termination time
     * @param ageThreshold
     *            The number of days that a snapshot is considered as cleanup candidate since it is created
     * @param retentionDays
     *            The number of days that the volume is retained before being terminated after being marked
     *            as cleanup candidate
     */
    public NoGeneratedAMIRule(MonkeyCalendar calendar, int ageThreshold, int retentionDays) {
    	this(calendar, ageThreshold, retentionDays, null);
    }

	/**
     * Constructor.
     *
     * @param calendar
     *            The calendar used to calculate the termination time
     * @param ageThreshold
     *            The number of days that a snapshot is considered as cleanup candidate since it is created
     * @param retentionDays
     *            The number of days that the volume is retained before being terminated after being marked
     *            as cleanup candidate
     * @param ownerEmailOverride
     *            If null, send notifications to the resource owner.
     *            If not null, send notifications to the provided owner email address instead of the resource owner.
     */
    public NoGeneratedAMIRule(MonkeyCalendar calendar, int ageThreshold, int retentionDays, String ownerEmailOverride) {
        Validate.notNull(calendar);
        Validate.isTrue(ageThreshold >= 0);
        Validate.isTrue(retentionDays >= 0);
        this.calendar = calendar;
        this.ageThreshold = ageThreshold;
        this.retentionDays = retentionDays;
        this.ownerEmailOverride = ownerEmailOverride;
    }
    
    @Override
    public boolean isValid(Resource resource) {
        Validate.notNull(resource);
        if (!resource.getResourceType().name().equals("EBS_SNAPSHOT")) {
            return true;
        }
        if (!"completed".equals(((AWSResource) resource).getAWSResourceState())) {
            return true;
        }
        String janitorTag = resource.getTag(JanitorMonkey.JANITOR_TAG);
        if (janitorTag != null) {
            if ("donotmark".equals(janitorTag)) {
                LOGGER.info(String.format("The snapshot %s is tagged as not handled by Janitor",
                        resource.getId()));
                return true;
            }
            try {
                // Owners can tag the volume with a termination date in the "janitor" tag.
                Date userSpecifiedDate = new Date(TERMINATION_DATE_FORMATTER.parseDateTime(janitorTag).getMillis());
                resource.setExpectedTerminationTime(userSpecifiedDate);
                resource.setTerminationReason(String.format("User specified termination date %s", janitorTag));
                if (ownerEmailOverride != null) {
                	resource.setOwnerEmail(ownerEmailOverride);
                }
                return false;
            } catch (Exception e) {
                LOGGER.error(String.format("The janitor tag is not a user specified date: %s", janitorTag));
            }
        }

        if (hasGeneratedImage(resource)) {
            return true;
        }

        if (resource.getLaunchTime() == null) {
            LOGGER.error(String.format("Snapshot %s does not have a creation time.", resource.getId()));
            return true;
        }
        DateTime launchTime = new DateTime(resource.getLaunchTime().getTime());
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        if (launchTime.plusDays(ageThreshold).isBefore(now)) {
            if (ownerEmailOverride != null) {
            	resource.setOwnerEmail(ownerEmailOverride);
            }
            if (resource.getExpectedTerminationTime() == null) {
                Date terminationTime = calendar.getBusinessDay(new Date(now.getMillis()), retentionDays);
                resource.setExpectedTerminationTime(terminationTime);
                resource.setTerminationReason(TERMINATION_REASON);
                LOGGER.info(String.format(
                        "Snapshot %s is marked to be cleaned at %s as there is no AMI generated using it",
                        resource.getId(), resource.getExpectedTerminationTime()));
            } else {
                LOGGER.info(String.format("Resource %s is already marked.", resource.getId()));
            }
            return false;
        }
        return true;

    }

    /**
     * Gets the AMI created using the snapshot. This method can be overridden by subclasses
     * if they use a different way to check this.
     * @param resource the snapshot resource
     * @return true if there are AMIs that are created using the snapshot, false otherwise
     */
    protected boolean hasGeneratedImage(Resource resource) {
        return StringUtils.isNotEmpty(resource.getAdditionalField(EBSSnapshotJanitorCrawler.SNAPSHOT_FIELD_AMIS));
    }

}

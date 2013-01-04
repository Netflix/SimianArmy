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
package com.netflix.simianarmy.aws.janitor.rule.volume;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.janitor.VolumeTaggingMonkey;
import com.netflix.simianarmy.janitor.JanitorMonkey;
import com.netflix.simianarmy.janitor.Rule;

/**
 * The rule is for checking whether an EBS volume is detached for more than
 * certain days. The rule mostly relies on tags on the volume to decide if
 * the volume should be marked.
 */
public class OldDetachedVolumeRule implements Rule {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OldDetachedVolumeRule.class);

    private final MonkeyCalendar calendar;

    private final int detachDaysThreshold;

    private final int retentionDays;

    /** The date format used to print or parse the user specified termination date. **/
    public static final DateTimeFormatter TERMINATION_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");


    /**
     * Constructor.
     *
     * @param calendar
     *            The calendar used to calculate the termination time
     * @param detachDaysThreshold
     *            The number of days that a volume is considered as cleanup candidate since it is detached
     * @param retentionDays
     *            The number of days that the volume is retained before being terminated after being marked
     *            as cleanup candidate
     */
    public OldDetachedVolumeRule(MonkeyCalendar calendar, int detachDaysThreshold, int retentionDays) {
        Validate.notNull(calendar);
        Validate.isTrue(detachDaysThreshold >= 0);
        Validate.isTrue(retentionDays >= 0);
        this.calendar = calendar;
        this.detachDaysThreshold = detachDaysThreshold;
        this.retentionDays = retentionDays;
    }

    @Override
    public boolean isValid(Resource resource) {
        Validate.notNull(resource);
        if (!resource.getResourceType().name().equals("EBS_VOLUME")) {
            return true;
        }
        if (!"available".equals(((AWSResource) resource).getAWSResourceState())) {
            return true;
        }
        String janitorTag = resource.getTag(JanitorMonkey.JANITOR_TAG);
        if (janitorTag != null) {
            if ("donotmark".equals(janitorTag)) {
                LOGGER.info(String.format("The volume %s is tagged as not handled by Janitor",
                        resource.getId()));
                return true;
            }
            try {
                // Owners can tag the volume with a termination date in the "janitor" tag.
                Date userSpecifiedDate = new Date(
                        TERMINATION_DATE_FORMATTER.parseDateTime(janitorTag).getMillis());
                resource.setExpectedTerminationTime(userSpecifiedDate);
                resource.setTerminationReason(String.format("User specified termination date %s", janitorTag));
                return false;
            } catch (Exception e) {
                LOGGER.error(String.format("The janitor tag is not a user specified date: %s", janitorTag));
            }
        }

        String janitorMetaTag = resource.getTag(JanitorMonkey.JANITOR_META_TAG);
        if (janitorMetaTag == null) {
            LOGGER.info(String.format("Volume %s is not tagged with the Janitor meta information, ignore.",
                    resource.getId()));
            return true;
        }

        Map<String, String> metadata = VolumeTaggingMonkey.parseJanitorMetaTag(janitorMetaTag);
        String detachTimeTag = metadata.get(JanitorMonkey.DETACH_TIME_TAG_KEY);
        if (detachTimeTag == null) {
            return true;
        }
        DateTime detachTime = null;
        try {
            detachTime = AWSResource.DATE_FORMATTER.parseDateTime(detachTimeTag);
        } catch (Exception e) {
            LOGGER.error(String.format("Detach time in the JANITOR_META tag of %s is not in the valid format: %s",
                    resource.getId(), detachTime));
            return true;
        }
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        if (detachTime != null && detachTime.plusDays(detachDaysThreshold).isBefore(now)) {
            if (resource.getExpectedTerminationTime() == null) {
                Date terminationTime = calendar.getBusinessDay(new Date(now.getMillis()), retentionDays);
                resource.setExpectedTerminationTime(terminationTime);
                resource.setTerminationReason(String.format("Volume not attached for %d days",
                        detachDaysThreshold + retentionDays));
                LOGGER.info(String.format(
                        "Volume %s is marked to be cleaned at %s as it is detached for more than %d days",
                        resource.getId(), resource.getExpectedTerminationTime(), detachDaysThreshold));
            } else {
                LOGGER.info(String.format("Resource %s is already marked.", resource.getId()));
            }
            return false;
        }
        return true;
    }
}

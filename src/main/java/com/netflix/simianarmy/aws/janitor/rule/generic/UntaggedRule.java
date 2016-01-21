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

package com.netflix.simianarmy.aws.janitor.rule.generic;

import java.util.Date;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.janitor.Rule;

/**
 * The rule for checking the orphaned instances that do not belong to any ASGs and
 * launched for certain days.
 */
public class UntaggedRule implements Rule {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UntaggedRule.class);

    private static final String TERMINATION_REASON = "This resource is missing the required tags";

    private final MonkeyCalendar calendar;

    private final Set<String> tagNames;

    private final int retentionDaysWithOwner;

    private final int retentionDaysWithoutOwner;


    /**
     * Constructor for UntaggedInstanceRule.
     *
     * @param calendar
     *            The calendar used to calculate the termination time
     * @param tagNames
     *            Set of tags that needs to be set
     */
    public UntaggedRule(MonkeyCalendar calendar, Set<String> tagNames, int retentionDaysWithOwner, int retentionDaysWithoutOwner) {
        Validate.notNull(calendar);
        Validate.notNull(tagNames);
        this.calendar = calendar;
        this.tagNames = tagNames;
        this.retentionDaysWithOwner = retentionDaysWithOwner;
        this.retentionDaysWithoutOwner = retentionDaysWithoutOwner;
    }

    @Override
    public boolean isValid(Resource resource) {
        Validate.notNull(resource);
        for (String tagName : this.tagNames) {
            if (((AWSResource) resource).getTag(tagName) == null) {
                String terminationReason = String.format(" does not have the required tag %s", tagName);
                LOGGER.error(String.format("The resource %s %s", resource.getId(), terminationReason));
                DateTime now = new DateTime(calendar.now().getTimeInMillis());
                if (resource.getExpectedTerminationTime() == null) {
                    int retentionDays = retentionDaysWithoutOwner;
                    if (resource.getOwnerEmail() != null) {
                        retentionDays = retentionDaysWithOwner;
                    }
                    Date terminationTime = calendar.getBusinessDay(new Date(now.getMillis()), retentionDays);
                    resource.setExpectedTerminationTime(terminationTime);
                    resource.setTerminationReason(terminationReason);
                }
                return false;
            } else {
                LOGGER.debug(String.format("The resource %s has the required tag %s", resource.getId(), tagName));
            }
        }
        LOGGER.info(String.format("The resource %s has all required tags", resource.getId()));
        return true;
    }
}

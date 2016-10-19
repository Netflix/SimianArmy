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

package com.netflix.simianarmy.aws.janitor.rule.elb;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.janitor.Rule;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * The rule for checking the orphaned instances that do not belong to any ASGs and
 * launched for certain days.
 */
public class OrphanedELBRule implements Rule {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanedELBRule.class);

    private static final String TERMINATION_REASON = "ELB has no instances and is not referenced by any ASG";
    private final MonkeyCalendar calendar;
    private final int retentionDays;

    /**
     * Constructor for OrphanedELBRule.
     *
     * @param calendar
     *            The calendar used to calculate the termination time
     * @param retentionDays
     *            The number of days that the marked ASG is retained before being terminated
     */
    public OrphanedELBRule(MonkeyCalendar calendar, int retentionDays) {
        Validate.notNull(calendar);
        Validate.isTrue(retentionDays >= 0);
        this.calendar = calendar;
        this.retentionDays = retentionDays;
    }

    @Override
    public boolean isValid(Resource resource) {
        Validate.notNull(resource);
        if (!resource.getResourceType().name().equals("ELB")) {
            return true;
        }

        String instanceCountStr = resource.getAdditionalField("instanceCount");
        String refASGCountStr = resource.getAdditionalField("referencedASGCount");
        if (StringUtils.isBlank(instanceCountStr)) {
            LOGGER.info(String.format("Resource %s is missing instance count, not marked as a cleanup candidate.", resource.getId()));
            return true;
        }
        if (StringUtils.isBlank(refASGCountStr)) {
            LOGGER.info(String.format("Resource %s is missing referenced ASG count, not marked as a cleanup candidate.", resource.getId()));
            return true;
        }

        int instanceCount = NumberUtils.toInt(instanceCountStr);
        int refASGCount = NumberUtils.toInt(refASGCountStr);
        if (instanceCount == 0 && refASGCount == 0) {
            LOGGER.info(String.format("Resource %s is marked as cleanup candidate with 0 instances and 0 referenced ASGs (owner: %s).", resource.getId(), resource.getOwnerEmail()));
            markResource(resource);
            return false;
        } else {
            LOGGER.info(String.format("Resource %s is not marked as cleanup candidate with %d instances and %d referenced ASGs.", resource.getId(), instanceCount, refASGCount));
            return true;
        }
    }

    private void markResource(Resource resource) {
        if (resource.getExpectedTerminationTime() == null) {
            Date terminationTime = calendar.getBusinessDay(new Date(), retentionDays);
            resource.setExpectedTerminationTime(terminationTime);
            resource.setTerminationReason(TERMINATION_REASON);
        } else {
            LOGGER.info(String.format("Resource %s is already marked as cleanup candidate.", resource.getId()));
        }
    }

}

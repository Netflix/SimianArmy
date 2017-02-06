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

package com.netflix.simianarmy.aws.janitor.rule.asg;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.janitor.crawler.ASGJanitorCrawler;
import com.netflix.simianarmy.janitor.Rule;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * The rule for detecting the ASGs that 1) have old launch configurations and
 * 2) do not have any instances or all instances are inactive in Eureka.
 * 3) are not fronted with any ELBs.
 */
public class SuspendedASGRule implements Rule {

    private final MonkeyCalendar calendar;
    private final int retentionDays;
    private final int suspensionAgeThreshold;
    private final ASGInstanceValidator instanceValidator;

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SuspendedASGRule.class);

    /**
     * Constructor.
     *
     * @param calendar
     *            The calendar used to calculate the termination time
     * @param retentionDays
     *            The number of days that the marked ASG is retained before being terminated after
     *            being marked
     * @param suspensionAgeThreshold
     *            The number of days that the ASG has been suspended from ELB that makes the ASG be
     *            considered a cleanup candidate
     * @param instanceValidator
     *            The instance validator to check if an instance is active
     */
    public SuspendedASGRule(MonkeyCalendar calendar, int suspensionAgeThreshold, int retentionDays,
                            ASGInstanceValidator instanceValidator) {
        Validate.notNull(calendar);
        Validate.isTrue(retentionDays >= 0);
        Validate.isTrue(suspensionAgeThreshold >= 0);
        Validate.notNull(instanceValidator);
        this.calendar = calendar;
        this.retentionDays = retentionDays;
        this.suspensionAgeThreshold = suspensionAgeThreshold;
        this.instanceValidator = instanceValidator;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid(Resource resource) {
        Validate.notNull(resource);
        if (!"ASG".equals(resource.getResourceType().name())) {
            return true;
        }

        if (instanceValidator.hasActiveInstance(resource)) {
            return true;
        }

        String suspensionTimeStr = resource.getAdditionalField(ASGJanitorCrawler.ASG_FIELD_SUSPENSION_TIME);
        if (!StringUtils.isEmpty(suspensionTimeStr)) {
            DateTime createTime = ASGJanitorCrawler.SUSPENSION_TIME_FORMATTER.parseDateTime(suspensionTimeStr);
            DateTime now = new DateTime(calendar.now().getTimeInMillis());
            if (now.isBefore(createTime.plusDays(suspensionAgeThreshold))) {
                LOGGER.info(String.format("The ASG %s has not been suspended for more than %d days",
                        resource.getId(), suspensionAgeThreshold));
                return true;
            }
            LOGGER.info(String.format("The ASG %s has been suspended for more than %d days",
                    resource.getId(), suspensionAgeThreshold));
            if (resource.getExpectedTerminationTime() == null) {
                Date terminationTime = calendar.getBusinessDay(new Date(now.getMillis()), retentionDays);
                resource.setExpectedTerminationTime(terminationTime);
                resource.setTerminationReason(String.format(
                        "ASG has been disabled for more than %d days and all instances are out of service in Discovery",
                        suspensionAgeThreshold + retentionDays));
            }
            return false;
        } else {
            LOGGER.info(String.format("ASG %s is not suspended from ELB.", resource.getId()));
            return true;
        }
    }
}

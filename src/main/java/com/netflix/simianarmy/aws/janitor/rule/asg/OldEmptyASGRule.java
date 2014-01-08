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
import com.netflix.simianarmy.aws.janitor.crawler.edda.EddaASGJanitorCrawler;
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
public class OldEmptyASGRule implements Rule {

    private final MonkeyCalendar calendar;
    private final int retentionDays;
    private final int launchConfigAgeThreshold;
    private final Integer lastChangeDaysThreshold;
    private final ASGInstanceValidator instanceValidator;

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OldEmptyASGRule.class);

    /**
     * Constructor.
     *
     * @param calendar
     *            The calendar used to calculate the termination time
     * @param retentionDays
     *            The number of days that the marked ASG is retained before being terminated
     * @param launchConfigAgeThreshold
     *            The number of days that the launch configuration for the ASG has been created that makes the ASG be
     *            considered obsolete
     * @param instanceValidator
     *            The instance validator to check if an instance is active
     */
    public OldEmptyASGRule(MonkeyCalendar calendar, int launchConfigAgeThreshold,
                           int retentionDays, ASGInstanceValidator instanceValidator) {
        this(calendar, launchConfigAgeThreshold, null, retentionDays, instanceValidator);
    }

    /**
     * Constructor.
     *
     * @param calendar
     *            The calendar used to calculate the termination time
     * @param retentionDays
     *            The number of days that the marked ASG is retained before being terminated
     * @param launchConfigAgeThreshold
     *            The number of days that the launch configuration for the ASG has been created that makes the ASG be
     *            considered obsolete
     * @param  lastChangeDaysThreshold
     *            The number of days that the launch configuration has not been changed. An ASG is considered as a
     *            cleanup candidate only if it has no change during the last n days. The parameter can be null.
     * @param instanceValidator
     *            The instance validator to check if an instance is active
     */
    public OldEmptyASGRule(MonkeyCalendar calendar, int launchConfigAgeThreshold, Integer lastChangeDaysThreshold,
                           int retentionDays, ASGInstanceValidator instanceValidator) {
        Validate.notNull(calendar);
        Validate.isTrue(retentionDays >= 0);
        Validate.isTrue(launchConfigAgeThreshold >= 0);
        Validate.isTrue(lastChangeDaysThreshold == null || lastChangeDaysThreshold >= 0);
        Validate.notNull(instanceValidator);
        this.calendar = calendar;
        this.retentionDays = retentionDays;
        this.launchConfigAgeThreshold = launchConfigAgeThreshold;
        this.lastChangeDaysThreshold = lastChangeDaysThreshold;
        this.instanceValidator = instanceValidator;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid(Resource resource) {
        Validate.notNull(resource);
        if (!"ASG".equals(resource.getResourceType().name())) {
            return true;
        }

        if (StringUtils.isNotEmpty(resource.getAdditionalField(ASGJanitorCrawler.ASG_FIELD_ELBS))) {
            LOGGER.info(String.format("ASG %s has ELBs.", resource.getId()));
            return true;
        }

        if (instanceValidator.hasActiveInstance(resource)) {
            LOGGER.info(String.format("ASG %s has active instance.", resource.getId()));
            return true;
        }

        String lcName = resource.getAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_NAME);
        DateTime now = new DateTime(calendar.now().getTimeInMillis());
        if (StringUtils.isEmpty(lcName)) {
            LOGGER.error(String.format("Failed to find launch configuration for ASG %s", resource.getId()));
            markResource(resource, now);
            return false;
        }

        String lcCreationTime = resource.getAdditionalField(ASGJanitorCrawler.ASG_FIELD_LC_CREATION_TIME);
        if (StringUtils.isEmpty(lcCreationTime)) {
            LOGGER.error(String.format("Failed to find creation time for launch configuration %s", lcName));
            return true;
        }

        DateTime createTime = new DateTime(Long.parseLong(lcCreationTime));
        if (now.isBefore(createTime.plusDays(launchConfigAgeThreshold))) {
            LOGGER.info(String.format("The launch configuration %s has not been created for more than %d days",
                    lcName, launchConfigAgeThreshold));
            return true;
        }
        LOGGER.info(String.format("The launch configuration %s has been created for more than %d days",
                lcName, launchConfigAgeThreshold));

        if (lastChangeDaysThreshold != null) {
            String lastChangeTimeField = resource.getAdditionalField(EddaASGJanitorCrawler.ASG_FIELD_LAST_CHANGE_TIME);
            if (StringUtils.isNotBlank(lastChangeTimeField)) {
                DateTime lastChangeTime = new DateTime(Long.parseLong(lastChangeTimeField));
                if (lastChangeTime.plusDays(lastChangeDaysThreshold).isAfter(now)) {
                    LOGGER.info(String.format("ASG %s had change during the last %d days",
                            resource.getId(), lastChangeDaysThreshold));
                    return true;
                }
            }
        }

        markResource(resource, now);
        return false;
    }

    private void markResource(Resource resource, DateTime now) {
        if (resource.getExpectedTerminationTime() == null) {
            Date terminationTime = calendar.getBusinessDay(new Date(now.getMillis()), retentionDays);
            resource.setExpectedTerminationTime(terminationTime);
            resource.setTerminationReason(String.format(
                    "Launch config older than %d days. Not in Discovery. No ELB.",
                    launchConfigAgeThreshold + retentionDays));
        } else {
            LOGGER.info(String.format("Resource %s is already marked as cleanup candidate.", resource.getId()));
        }
    }
}

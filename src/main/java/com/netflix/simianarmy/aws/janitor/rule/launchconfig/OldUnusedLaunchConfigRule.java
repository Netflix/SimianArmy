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

package com.netflix.simianarmy.aws.janitor.rule.launchconfig;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.janitor.crawler.LaunchConfigJanitorCrawler;
import com.netflix.simianarmy.janitor.Rule;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * The rule for detecting the launch configurations that
 * 1) have been created for certain days and
 * 2) are not used by any auto scaling groups.
 */
public class OldUnusedLaunchConfigRule implements Rule {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OldUnusedLaunchConfigRule.class);

    private static final String TERMINATION_REASON = "Launch config is not used by any ASG";

    private final MonkeyCalendar calendar;

    private final int ageThreshold;

    private final int retentionDays;

    /**
     * Constructor for OrphanedInstanceRule.
     *
     * @param calendar
     *            The calendar used to calculate the termination time
     * @param ageThreshold
     *            The number of days that a launch configuration is considered as a cleanup candidate
     *            since it is created
     * @param retentionDays
     *            The number of days that the unused launch configuration is retained before being terminated
     */
    public OldUnusedLaunchConfigRule(MonkeyCalendar calendar, int ageThreshold, int retentionDays) {
        Validate.notNull(calendar);
        Validate.isTrue(ageThreshold >= 0);
        Validate.isTrue(retentionDays >= 0);
        this.calendar = calendar;
        this.ageThreshold = ageThreshold;
        this.retentionDays = retentionDays;
    }

    @Override
    public boolean isValid(Resource resource) {
        Validate.notNull(resource);
        if (!"LAUNCH_CONFIG".equals(resource.getResourceType().name())) {
            return true;
        }
        AWSResource lcResource = (AWSResource) resource;
        String usedByASG = lcResource.getAdditionalField(LaunchConfigJanitorCrawler.LAUNCH_CONFIG_FIELD_USED_BY_ASG);
        if (StringUtils.isNotEmpty(usedByASG) && !Boolean.parseBoolean(usedByASG)) {
            if (resource.getLaunchTime() == null) {
                LOGGER.error(String.format("The launch config %s has no creation time.", resource.getId()));
                return true;
            } else {
                DateTime launchTime = new DateTime(resource.getLaunchTime().getTime());
                DateTime now = new DateTime(calendar.now().getTimeInMillis());
                if (now.isBefore(launchTime.plusDays(ageThreshold))) {
                    LOGGER.info(String.format("The unused launch config %s has not been created for more than %d days",
                            resource.getId(), ageThreshold));
                    return true;
                }
                LOGGER.info(String.format("The unused launch config %s has been created for more than %d days",
                        resource.getId(), ageThreshold));
                if (resource.getExpectedTerminationTime() == null) {
                    Date terminationTime = calendar.getBusinessDay(new Date(now.getMillis()), retentionDays);
                    resource.setExpectedTerminationTime(terminationTime);
                    resource.setTerminationReason(TERMINATION_REASON);
                }
                return false;
            }
        }
        return true;
    }

}

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

package com.netflix.simianarmy.aws.janitor.rule.instance;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.janitor.crawler.InstanceJanitorCrawler;
import com.netflix.simianarmy.janitor.Rule;

/**
 * The rule for checking the orphaned instances that do not belong to any ASGs and
 * launched for certain days.
 */
public class OrphanedInstanceRule implements Rule {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanedInstanceRule.class);

    private static final String TERMINATION_REASON = "No ASG is associated with this instance";
    private static final String ASG_OR_OPSWORKS_TERMINATION_REASON = "No ASG or OpsWorks stack is associated with this instance";

    private final MonkeyCalendar calendar;

    private final int instanceAgeThreshold;

    private final int retentionDaysWithOwner;

    private final int retentionDaysWithoutOwner;
    
    private final boolean respectOpsWorksParentage;

    /**
     * Constructor for OrphanedInstanceRule.
     *
     * @param calendar
     *            The calendar used to calculate the termination time
     * @param instanceAgeThreshold
     *            The number of days that an instance is considered as orphaned since it is launched
     * @param retentionDaysWithOwner
     *            The number of days that the orphaned instance is retained before being terminated
     *            when the instance has an owner specified
     * @param retentionDaysWithoutOwner
     *            The number of days that the orphaned instance is retained before being terminated
     *            when the instance has no owner specified
     * @param respectOpsWorksParentage
     *            If true, don't consider members of an OpsWorks stack as orphans 
     */
    public OrphanedInstanceRule(MonkeyCalendar calendar,
            int instanceAgeThreshold, int retentionDaysWithOwner, int retentionDaysWithoutOwner, boolean respectOpsWorksParentage) {
        Validate.notNull(calendar);
        Validate.isTrue(instanceAgeThreshold >= 0);
        Validate.isTrue(retentionDaysWithOwner >= 0);
        Validate.isTrue(retentionDaysWithoutOwner >= 0);
        this.calendar = calendar;
        this.instanceAgeThreshold = instanceAgeThreshold;
        this.retentionDaysWithOwner = retentionDaysWithOwner;
        this.retentionDaysWithoutOwner = retentionDaysWithoutOwner;
        this.respectOpsWorksParentage = respectOpsWorksParentage;
    }
    
    public OrphanedInstanceRule(MonkeyCalendar calendar,
            int instanceAgeThreshold, int retentionDaysWithOwner, int retentionDaysWithoutOwner) {
        this(calendar, instanceAgeThreshold, retentionDaysWithOwner, retentionDaysWithoutOwner, false);
    }

    @Override
    public boolean isValid(Resource resource) {
        Validate.notNull(resource);
        if (!resource.getResourceType().name().equals("INSTANCE")) {
            // The rule is supposed to only work on AWS instances. If a non-instance resource
            // is passed to the rule, the rule simply ignores it and considers it as a valid
            // resource not for cleanup.
            return true;
        }
        String awsStatus = ((AWSResource) resource).getAWSResourceState();
        if (!"running".equals(awsStatus) || "pending".equals(awsStatus)) {
            return true;
        }
        AWSResource instanceResource = (AWSResource) resource;
        String asgName = instanceResource.getAdditionalField(InstanceJanitorCrawler.INSTANCE_FIELD_ASG_NAME);
        String opsworkStackName = instanceResource.getAdditionalField(InstanceJanitorCrawler.INSTANCE_FIELD_OPSWORKS_STACK_NAME);
        // If there is no ASG AND it isn't an OpsWorks stack (or OpsWorks isn't respected as a parent), we have an orphan
        if (StringUtils.isEmpty(asgName) && (!respectOpsWorksParentage || StringUtils.isEmpty(opsworkStackName))) {
            if (resource.getLaunchTime() == null) {
                LOGGER.error(String.format("The instance %s has no launch time.", resource.getId()));
                return true;
            } else {
                DateTime launchTime = new DateTime(resource.getLaunchTime().getTime());
                DateTime now = new DateTime(calendar.now().getTimeInMillis());
                if (now.isBefore(launchTime.plusDays(instanceAgeThreshold))) {
                    LOGGER.info(String.format("The orphaned instance %s has not launched for more than %d days",
                            resource.getId(), instanceAgeThreshold));
                    return true;
                }
                LOGGER.info(String.format("The orphaned instance %s has launched for more than %d days",
                        resource.getId(), instanceAgeThreshold));
                if (resource.getExpectedTerminationTime() == null) {
                    int retentionDays = retentionDaysWithoutOwner;
                    if (resource.getOwnerEmail() != null) {
                        retentionDays = retentionDaysWithOwner;
                    }
                    Date terminationTime = calendar.getBusinessDay(new Date(now.getMillis()), retentionDays);
                    resource.setExpectedTerminationTime(terminationTime);
                    resource.setTerminationReason((respectOpsWorksParentage) ? ASG_OR_OPSWORKS_TERMINATION_REASON : TERMINATION_REASON);
                }
                return false;
            }
        }
        return true;
    }
}

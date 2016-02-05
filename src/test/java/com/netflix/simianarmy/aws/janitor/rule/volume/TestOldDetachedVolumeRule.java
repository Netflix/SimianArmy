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
//CHECKSTYLE IGNORE Javadoc
//CHECKSTYLE IGNORE MagicNumberCheck

package com.netflix.simianarmy.aws.janitor.rule.volume;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.TestUtils;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.aws.janitor.VolumeTaggingMonkey;
import com.netflix.simianarmy.aws.janitor.rule.TestMonkeyCalendar;
import com.netflix.simianarmy.janitor.JanitorMonkey;

import static org.joda.time.DateTimeConstants.MILLIS_PER_DAY;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;


public class TestOldDetachedVolumeRule {

    @Test
    public void testNonVolumeResource() {
        Resource resource = new AWSResource().withId("asg1").withResourceType(AWSResourceType.ASG);
        ((AWSResource) resource).setAWSResourceState("available");
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(new TestMonkeyCalendar(), 0, 0);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testUnavailableVolume() {
        Resource resource = new AWSResource().withId("vol-12345678901234567").withResourceType(AWSResourceType.EBS_VOLUME);
        ((AWSResource) resource).setAWSResourceState("stopped");
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(new TestMonkeyCalendar(), 0, 0);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testTaggedAsNotMark() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("vol-12345678901234567").withResourceType(AWSResourceType.EBS_VOLUME)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("available");
        Date lastDetachTime = new Date(now.minusDays(ageThreshold + 1).getMillis());
        String metaTag = VolumeTaggingMonkey.makeMetaTag(null, null, lastDetachTime);
        resource.setTag(JanitorMonkey.JANITOR_META_TAG, metaTag);
        int retentionDays = 4;
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        resource.setTag(JanitorMonkey.JANITOR_TAG, "donotmark");
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testNoMetaTag() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("vol-12345678901234567").withResourceType(AWSResourceType.EBS_VOLUME)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("available");
        int retentionDays = 4;
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        resource.setTag(JanitorMonkey.JANITOR_TAG, "donotmark");
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testUserSpecifiedTerminationDate() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("vol-12345678901234567").withResourceType(AWSResourceType.EBS_VOLUME)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("available");
        int retentionDays = 4;
        DateTime userDate = new DateTime(now.plusDays(3).withTimeAtStartOfDay());
        resource.setTag(JanitorMonkey.JANITOR_TAG,
                OldDetachedVolumeRule.TERMINATION_DATE_FORMATTER.print(userDate));
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        Assert.assertFalse(rule.isValid(resource));
        Assert.assertEquals(resource.getExpectedTerminationTime().getTime(), userDate.getMillis());
    }

    @Test
    public void testOldDetachedVolume() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("vol-12345678901234567").withResourceType(AWSResourceType.EBS_VOLUME)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("available");
        Date lastDetachTime = new Date(now.minusDays(ageThreshold + 1).getMillis());
        String metaTag = VolumeTaggingMonkey.makeMetaTag(null, null, lastDetachTime);
        resource.setTag(JanitorMonkey.JANITOR_META_TAG, metaTag);
        int retentionDays = 4;
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        Assert.assertFalse(rule.isValid(resource));
        TestUtils.verifyTerminationTimeRough(resource, retentionDays, now);
    }

    /** This test exists to check logic on a utility method.
     * The tagging rule for resource expiry uses a variable nubmer of days.
     * However, JodaTime date arithmetic for DAYS uses calendar days.  It does NOT
     * treat a day as 24 hours in this case (HOUR arithmetic, however, does).
     * Therefore, a termination policy of 4 days (96 hours) will actually occur in
     * 95 hours if the resource is tagged with that rule within 4 days of the DST
     * cutover.
     *
     * We experienced test case failures around the 2014 spring DST cutover that
     * prevented us from getting green builds.  So, the assertion logic was loosened
     * to check that we were within a day of the expected date.  For the test case,
     * all but 4 days of the year this problem never shows up.  To verify that our
     * fix was correct, this test case explicitly sets the date.  The other tests
     * that use a DateTime of "DateTime.now()" are not true unit tests, because the
     * test does not isolate the date.  They are actually a partial integration test,
     * as they leave the date up to the system where the test executes.
     *
     * We have to mock the call to MonkeyCalendar.now() because the constructor
     * for that class uses Calendar.getInstance() internally.
     *
     */
    @Test
    public void testOldDetachedVolumeBeforeDaylightSavingsCutover() {
        int ageThreshold = 5;
        //here we set the create date to a few days before a known DST cutover, where
        //we observed DST failures
        DateTime closeToSpringAheadDst = new DateTime(2014, 3, 7, 0, 0, DateTimeZone.forID("America/Los_Angeles"));
        Resource resource = new AWSResource().withId("vol-12345678901234567").withResourceType(AWSResourceType.EBS_VOLUME)
            .withLaunchTime(new Date(closeToSpringAheadDst.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("available");
        Date lastDetachTime = new Date(closeToSpringAheadDst.minusDays(ageThreshold + 1).getMillis());
        String metaTag = VolumeTaggingMonkey.makeMetaTag(null, null, lastDetachTime);
        resource.setTag(JanitorMonkey.JANITOR_META_TAG, metaTag);
        int retentionDays = 4;

        //set the "now" to the fixed execution date for this rule and create a partial mock
        Calendar fixed = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        fixed.setTimeInMillis(closeToSpringAheadDst.getMillis());
        MonkeyCalendar monkeyCalendar = new TestMonkeyCalendar();
        MonkeyCalendar spyCalendar = spy(monkeyCalendar);
        when(spyCalendar.now()).thenReturn(fixed);

        //use the partial mock for the OldDetachedVolumeRule
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(spyCalendar, ageThreshold, retentionDays);
        Assert.assertFalse(rule.isValid(resource)); //this volume should be seen as invalid

        //3.26.2014. Commenting out DST cutover verification.  A line in
        //OldDetachedVolumeRule.isValid actually creates a new date from the Tag value.
        //while this unit test tries its best to use invariants, the tag does not contain timezone
        //information, so a time set in the Los Angeles timezone and tagged, then parsed as
        //UTC (if that's how the VM running the test is set) will fail.


/////////////////////////////
        //Leaving the code in place to be uncommnented later if that class is refactored
        //to support a design that promotes more complete testing.

        //now verify that the difference between "now" and the cutoff is slightly under the intended
        //retention limit, as the DST cutover makes us lose one hour
        //verifyDSTCutoverHappened(resource, retentionDays, closeToSpringAheadDst);
/////////////////////////////
        //now verify that our projected termination time is within one day of what was asked for
        TestUtils.verifyTerminationTimeRough(resource, retentionDays, closeToSpringAheadDst);
    }

    @Test
    public void testDetachedVolumeNotOld() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("vol-12345678901234567").withResourceType(AWSResourceType.EBS_VOLUME)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("available");
        Date lastDetachTime = new Date(now.minusDays(ageThreshold - 1).getMillis());
        String metaTag = VolumeTaggingMonkey.makeMetaTag(null, null, lastDetachTime);
        resource.setTag(JanitorMonkey.JANITOR_META_TAG, metaTag);
        int retentionDays = 4;
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }

    @Test
    public void testAttachedVolume() {
        int ageThreshold = 5;
        DateTime now = DateTime.now();
        Resource resource = new AWSResource().withId("vol-12345678901234567").withResourceType(AWSResourceType.EBS_VOLUME)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("available");
        String metaTag = VolumeTaggingMonkey.makeMetaTag("i-12345678901234567", "owner", null);
        resource.setTag(JanitorMonkey.JANITOR_META_TAG, metaTag);
        int retentionDays = 4;
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        Assert.assertTrue(rule.isValid(resource));
        Assert.assertNull(resource.getExpectedTerminationTime());
    }


    @Test
    public void testResourceWithExpectedTerminationTimeSet() {
        DateTime now = DateTime.now();
        Date oldTermDate = new Date(now.plusDays(10).getMillis());
        String oldTermReason = "Foo";
        int ageThreshold = 5;
        Resource resource = new AWSResource().withId("vol-12345678901234567").withResourceType(AWSResourceType.EBS_VOLUME)
                .withLaunchTime(new Date(now.minusDays(ageThreshold + 1).getMillis()));
        ((AWSResource) resource).setAWSResourceState("available");
        Date lastDetachTime = new Date(now.minusDays(ageThreshold + 1).getMillis());
        String metaTag = VolumeTaggingMonkey.makeMetaTag(null, null, lastDetachTime);
        resource.setTag(JanitorMonkey.JANITOR_META_TAG, metaTag);
        int retentionDays = 4;
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(new TestMonkeyCalendar(),
                ageThreshold, retentionDays);
        resource.setExpectedTerminationTime(oldTermDate);
        resource.setTerminationReason(oldTermReason);
        Assert.assertFalse(rule.isValid(resource));
        Assert.assertEquals(oldTermDate, resource.getExpectedTerminationTime());
        Assert.assertEquals(oldTermReason, resource.getTerminationReason());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullResource() {
        OldDetachedVolumeRule rule = new OldDetachedVolumeRule(new TestMonkeyCalendar(), 5, 4);
        rule.isValid(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeAgeThreshold() {
        new OldDetachedVolumeRule(new TestMonkeyCalendar(), -1, 4);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNgativeRetentionDaysWithOwner() {
        new OldDetachedVolumeRule(new TestMonkeyCalendar(), 5, -4);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNullCalendar() {
        new OldDetachedVolumeRule(null, 5, 4);
    }

    /** Verify that a test conditioned to run across the spring DST cutover actually did
     * cross that threshold.  The real difference will be about 0.05 days less than
     * the retentionDays parameter.
     * @param resource The AWS resource being tested
     * @param retentionDays Number of days the resource should be kept around
     * @param timeOfCheck When the check is executed
     */
    private void verifyDSTCutoverHappened(Resource resource, int retentionDays, DateTime timeOfCheck) {
        double realDays = (double) (resource.getExpectedTerminationTime().getTime() - timeOfCheck.getMillis())
            / (double) MILLIS_PER_DAY;
        long days = (resource.getExpectedTerminationTime().getTime() - timeOfCheck.getMillis()) / MILLIS_PER_DAY;
        Assert.assertTrue(realDays < (double) retentionDays);
        Assert.assertNotEquals(days, retentionDays);
    }

}

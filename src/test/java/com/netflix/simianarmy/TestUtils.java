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
package com.netflix.simianarmy;

import static org.joda.time.DateTimeConstants.MILLIS_PER_DAY;

import org.joda.time.DateTime;
import org.testng.Assert;

/** Utility class for test cases.
 * @author mgeis
 *
 */
public final class TestUtils {

    private TestUtils() {
        //this should never be called
        //if called internally, throw an error
        throw new InstantiationError("Instantiation of TestUtils utility class prohibited.");
    }

    /** Verify that the termination date is roughly retentionDays from now
     * By 'roughly' we mean within one day.  There are times (twice per year)
     * when certain tests execute and the Daylight Savings cutover makes it not
     * a precisely rounded day amount (for example, a termination policy of 4 days
     * will really be about 3.95 days, or 95 hours, because one hour is lost as
     * the clocks "spring ahead").
     *
     * A more precise, but complicated logic could be written to make sure that "roughly"
     * means not more than an hour before and not more than an hour after the anticipated
     * cutoff, but that makes the test much less readable.
     *
     * By just making sure that the difference between the actual and proposed dates
     * is less than one day, we get a rough idea of whether the termination time was correct.
     * @param resource The AWS Resource to be checked
     * @param retentionDays number of days it should be kept around
     * @param timeOfCheck The time the check is run
     */
    public static void verifyTerminationTimeRough(Resource resource, int retentionDays, DateTime timeOfCheck) {
        long days = (resource.getExpectedTerminationTime().getTime() - timeOfCheck.getMillis()) / MILLIS_PER_DAY;
        Assert.assertTrue(Math.abs(days - retentionDays) <= 1);
    }

}

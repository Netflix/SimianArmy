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
package com.netflix.simianarmy.aws;

import org.testng.Assert;
import org.testng.annotations.Test;

// CHECKSTYLE IGNORE MagicNumberCheck
public class TestAWSEmailNotifier extends AWSEmailNotifier {
    public TestAWSEmailNotifier() {
        super(null);
    }

    @Override
    public String buildEmailSubject(String to) {
        return null;
    }

    @Override
    public String[] getCcAddresses(String to) {
        return new String[0];
    }

    @Override
    public String getSourceAddress(String to) {
        return null;
    }

    @Test
    public void testEmailWithHashIsValid() {
        TestAWSEmailNotifier emailNotifier = new TestAWSEmailNotifier();
        Assert.assertTrue(emailNotifier.isValidEmail("#bla-#name@domain-test.com"), "Email with hash is not valid");
    }
}

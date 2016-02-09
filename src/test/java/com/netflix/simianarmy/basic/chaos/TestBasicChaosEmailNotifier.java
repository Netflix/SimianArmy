/*
 *
 *  Copyright 2013 Netflix, Inc.
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
// CHECKSTYLE IGNORE Javadoc
package com.netflix.simianarmy.basic.chaos;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext.TestInstanceGroup;

public class TestBasicChaosEmailNotifier {

    private final AmazonSimpleEmailServiceClient sesClient = new AmazonSimpleEmailServiceClient();

    private BasicChaosEmailNotifier basicChaosEmailNotifier;

    private Properties properties;

    private enum GroupTypes implements GroupType {
        TYPE_A
    };

    private String name = "name0";
    private String region = "reg1";
    private String to = "foo@bar.com";
    private String instanceId = "i-12345678901234567";
    private String subjectPrefix = "Subject Prefix - ";
    private String subjectSuffix = " - Subject Suffix ";
    private String bodyPrefix = "Body Prefix - ";
    private String bodySuffix = " - Body Suffix";

    private final TestInstanceGroup testInstanceGroup = new TestInstanceGroup(GroupTypes.TYPE_A, name, region, "0:"
            + instanceId);

    private String defaultBody = "Instance " + instanceId + " of " + GroupTypes.TYPE_A + " " + name
            + " is being terminated by Chaos monkey.";

    private String defaultSubject = "Chaos Monkey Termination Notification for " + to;

    @BeforeMethod
    public void beforeMethod() {
        properties = new Properties();
    }

    @Test
    public void testInvalidEmailAddresses() {
        String[] invalidEmails = new String[] { "username",
                                                "username@.com.my",
                                                "username123@example.a",
                                                "username123@.com",
                                                "username123@.com.com",
                                                "username()*@example.com",
                                                "username@%*.com"};
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), sesClient, null);

        for (String emailAddress : invalidEmails) {
            Assert.assertFalse(basicChaosEmailNotifier.isValidEmail(emailAddress));
        }
    }

    @Test
    public void testValidEmailAddresses() {
        String[] validEmails = new String[] { "username-100@example.com",
                                              "name.surname+ml-info@example.com",
                                              "username.100@example.com",
                                              "username111@example.com",
                                              "username-100@username.net",
                                              "username.100@example.com.au",
                                              "username@1.com",
                                              "username@example.com",
                                              "username+100@example.com",
                                              "username-100@example-test.com" };
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), sesClient, null);

        for (String emailAddress : validEmails) {
            Assert.assertTrue(basicChaosEmailNotifier.isValidEmail(emailAddress));
        }
    }

    @Test
    public void testbuildEmailSubject() {
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), sesClient, null);
        String subject = basicChaosEmailNotifier.buildEmailSubject(to);
        Assert.assertEquals(subject, defaultSubject);
    }

    @Test
    public void testbuildEmailSubjectWithSubjectPrefix() {
        properties.setProperty("simianarmy.chaos.notification.subject.prefix", subjectPrefix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), sesClient, null);
        String subject = basicChaosEmailNotifier.buildEmailSubject(to);
        Assert.assertEquals(subject, subjectPrefix + defaultSubject);
    }

    @Test
    public void testbuildEmailSubjectWithSubjectSuffix() {
        properties.setProperty("simianarmy.chaos.notification.subject.suffix", subjectSuffix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), sesClient, null);
        String subject = basicChaosEmailNotifier.buildEmailSubject(to);
        Assert.assertEquals(subject, defaultSubject + subjectSuffix);
    }

    @Test
    public void testbuildEmailSubjectWithSubjectPrefixSuffix() {
        properties.setProperty("simianarmy.chaos.notification.subject.prefix", subjectPrefix);
        properties.setProperty("simianarmy.chaos.notification.subject.suffix", subjectSuffix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), sesClient, null);
        String subject = basicChaosEmailNotifier.buildEmailSubject(to);
        Assert.assertEquals(subject, subjectPrefix + defaultSubject + subjectSuffix);
    }

    @Test
    public void testbuildEmailBody() {
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), sesClient, null);
        String subject = basicChaosEmailNotifier.buildEmailBody(testInstanceGroup, instanceId, null);
        Assert.assertEquals(subject, defaultBody);
    }

    @Test
    public void testbuildEmailBodyPrefix() {
        properties.setProperty("simianarmy.chaos.notification.body.prefix", bodyPrefix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), sesClient, null);
        String subject = basicChaosEmailNotifier.buildEmailBody(testInstanceGroup, instanceId, null);
        Assert.assertEquals(subject, bodyPrefix + defaultBody);
    }

    @Test
    public void testbuildEmailBodySuffix() {
        properties.setProperty("simianarmy.chaos.notification.body.suffix", bodySuffix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), sesClient, null);
        String subject = basicChaosEmailNotifier.buildEmailBody(testInstanceGroup, instanceId, null);
        Assert.assertEquals(subject, defaultBody + bodySuffix);
    }

    @Test
    public void testbuildEmailBodyPrefixSuffix() {
        properties.setProperty("simianarmy.chaos.notification.body.prefix", bodyPrefix);
        properties.setProperty("simianarmy.chaos.notification.body.suffix", bodySuffix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), sesClient, null);
        String subject = basicChaosEmailNotifier.buildEmailBody(testInstanceGroup, instanceId, null);
        Assert.assertEquals(subject, bodyPrefix + defaultBody + bodySuffix);
    }

    @Test
    public void testBuildAndSendEmail() {
        properties.setProperty("simianarmy.chaos.notification.sourceEmail", to);
        BasicChaosEmailNotifier spyBasicChaosEmailNotifier = spy(new BasicChaosEmailNotifier(new BasicConfiguration(
                properties), sesClient, null));
        doNothing().when(spyBasicChaosEmailNotifier).sendEmail(to, defaultSubject, defaultBody);
        spyBasicChaosEmailNotifier.buildAndSendEmail(to, testInstanceGroup, instanceId, null);
        verify(spyBasicChaosEmailNotifier).sendEmail(to, defaultSubject, defaultBody);
    }

    @Test
    public void testBuildAndSendEmailSubjectIsBody() {
        properties.setProperty("simianarmy.chaos.notification.subject.isBody", "true");
        properties.setProperty("simianarmy.chaos.notification.sourceEmail", to);
        BasicChaosEmailNotifier spyBasicChaosEmailNotifier = spy(new BasicChaosEmailNotifier(new BasicConfiguration(
                properties), sesClient, null));
        doNothing().when(spyBasicChaosEmailNotifier).sendEmail(to, defaultBody, defaultBody);
        spyBasicChaosEmailNotifier.buildAndSendEmail(to, testInstanceGroup, instanceId, null);
        verify(spyBasicChaosEmailNotifier).sendEmail(to, defaultBody, defaultBody);
    }

}

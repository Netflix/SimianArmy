// CHECKSTYLE IGNORE Javadoc
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
package com.netflix.simianarmy.basic.chaos;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Properties;


import org.subethamail.wiser.Wiser;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.netflix.simianarmy.GroupType;
import com.netflix.simianarmy.aws.AWSEmailClient;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.basic.JakartaCommonsEmailClient;
import com.netflix.simianarmy.chaos.TestChaosMonkeyContext.TestInstanceGroup;

public class TestBasicChaosEmailNotifier {

    private BasicChaosEmailNotifier basicChaosEmailNotifier;

    private Properties properties;

    private enum GroupTypes implements GroupType {
        TYPE_A
    };

    private String name = "name0";
    private String region = "reg1";
    private String to = "foo@bar.com";
    private String instanceId = "i-123456780";
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
    public void testbuildEmailSubject() {
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), null);
        String subject = basicChaosEmailNotifier.buildEmailSubject(to);
        Assert.assertEquals(subject, defaultSubject);
    }

    @Test
    public void testbuildEmailSubjectWithSubjectPrefix() {
        properties.setProperty("simianarmy.chaos.notification.subject.prefix", subjectPrefix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), null);
        String subject = basicChaosEmailNotifier.buildEmailSubject(to);
        Assert.assertEquals(subject, subjectPrefix + defaultSubject);
    }

    @Test
    public void testbuildEmailSubjectWithSubjectSuffix() {
        properties.setProperty("simianarmy.chaos.notification.subject.suffix", subjectSuffix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), null);
        String subject = basicChaosEmailNotifier.buildEmailSubject(to);
        Assert.assertEquals(subject, defaultSubject + subjectSuffix);
    }

    @Test
    public void testbuildEmailSubjectWithSubjectPrefixSuffix() {
        properties.setProperty("simianarmy.chaos.notification.subject.prefix", subjectPrefix);
        properties.setProperty("simianarmy.chaos.notification.subject.suffix", subjectSuffix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), null);
        String subject = basicChaosEmailNotifier.buildEmailSubject(to);
        Assert.assertEquals(subject, subjectPrefix + defaultSubject + subjectSuffix);
    }

    @Test
    public void testbuildEmailBody() {
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), null);
        String subject = basicChaosEmailNotifier.buildEmailBody(testInstanceGroup, instanceId, null);
        Assert.assertEquals(subject, defaultBody);
    }

    @Test
    public void testbuildEmailBodyPrefix() {
        properties.setProperty("simianarmy.chaos.notification.body.prefix", bodyPrefix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), null);
        String subject = basicChaosEmailNotifier.buildEmailBody(testInstanceGroup, instanceId, null);
        Assert.assertEquals(subject, bodyPrefix + defaultBody);
    }

    @Test
    public void testbuildEmailBodySuffix() {
        properties.setProperty("simianarmy.chaos.notification.body.suffix", bodySuffix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), null);
        String subject = basicChaosEmailNotifier.buildEmailBody(testInstanceGroup, instanceId, null);
        Assert.assertEquals(subject, defaultBody + bodySuffix);
    }

    @Test
    public void testbuildEmailBodyPrefixSuffix() {
        properties.setProperty("simianarmy.chaos.notification.body.prefix", bodyPrefix);
        properties.setProperty("simianarmy.chaos.notification.body.suffix", bodySuffix);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(properties), null);
        String subject = basicChaosEmailNotifier.buildEmailBody(testInstanceGroup, instanceId, null);
        Assert.assertEquals(subject, bodyPrefix + defaultBody + bodySuffix);
    }

    /**
     * Test to verify that specifying a specific email client implementation works and that
     * the default is not selected
     */
    @Test
    public void testUsesJakartaCommonsEmailClient() {
        properties.setProperty("simianarmy.chaos.notification.sourceEmail", to);
        properties.setProperty("simianarmy.client.email.class",
            "com.netflix.simianarmy.basic.JakartaCommonsEmailClient");
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(
            properties), null);
        Assert.assertEquals(basicChaosEmailNotifier.getEmailClient().getClass(),
            JakartaCommonsEmailClient.class);
    }

    /**
     * Test to verify that NOT specifying a specific email causes the default to be selected
     */
    @Test
    public void testUsesAWSEmailClient() {
        properties.setProperty("simianarmy.chaos.notification.sourceEmail", to);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(
            properties), null);
        Assert.assertEquals(basicChaosEmailNotifier.getEmailClient().getClass(),
            AWSEmailClient.class);
    }

    /**
     * Test that the SMTP client actually works and sends an email. Rather than using mocks of a lower level
     * class that is essentially auto-wired, this is more of an integration test.  In the test,
     * we spin up an in-memory SMTP server running on port 2500 (if you run it on 25, the default SMTP port,
     * you may see a SecurityException because you're trying to run a server on a low-numbered port).
     * The server does not deliver mail.  It stores it for examination at the end of the test.
     */
    @Test
    public void testBuildAndSendEmailForReal() {
        Wiser wiser = new Wiser();
        try {
            wiser.setPort(2500); // Default is 25
            wiser.start();
            properties.setProperty("simianarmy.chaos.notification.sourceEmail", "mightyjoeyoung@simianarmy.com");
            properties.setProperty("simianarmy.client.email.class",
                "com.netflix.simianarmy.basic.JakartaCommonsEmailClient");
            properties.setProperty("simianarmy.client.smtp.port", "2500");
            String toReal = "bonzo@simianarmy.com";
            basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(
                properties), null);
            BasicChaosEmailNotifier spyBasicChaosEmailNotifier = spy(basicChaosEmailNotifier);
            spyBasicChaosEmailNotifier.buildAndSendEmail(toReal, testInstanceGroup, instanceId, null);
            verify(spyBasicChaosEmailNotifier).buildAndSendEmail(toReal, testInstanceGroup, instanceId, null);
            Assert.assertEquals(wiser.getMessages().size(), 1);
        } finally {
            wiser.stop();
        }
    }

    @Test
    public void testBuildAndSendEmail() {
        String toReal = "bonzo@simianarmy.com";
        properties.setProperty("simianarmy.chaos.notification.sourceEmail", toReal);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(
            properties), null);
        BasicChaosEmailNotifier spyBasicChaosEmailNotifier = spy(basicChaosEmailNotifier);
        doNothing().when(spyBasicChaosEmailNotifier).buildAndSendEmail(toReal, testInstanceGroup, instanceId, null);
        spyBasicChaosEmailNotifier.buildAndSendEmail(toReal, testInstanceGroup, instanceId, null);
        verify(spyBasicChaosEmailNotifier).buildAndSendEmail(toReal, testInstanceGroup, instanceId, null);
    }

    @Test
    public void testBuildAndSendEmailSubjectIsBody() {
        properties.setProperty("simianarmy.chaos.notification.subject.isBody", "true");
        properties.setProperty("simianarmy.chaos.notification.sourceEmail", to);
        basicChaosEmailNotifier = new BasicChaosEmailNotifier(new BasicConfiguration(
            properties), null);
        BasicChaosEmailNotifier spyBasicChaosEmailNotifier = spy(basicChaosEmailNotifier);
        doNothing().when(spyBasicChaosEmailNotifier).buildAndSendEmail(to, testInstanceGroup, instanceId, null);
        spyBasicChaosEmailNotifier.buildAndSendEmail(to, testInstanceGroup, instanceId, null);
        verify(spyBasicChaosEmailNotifier).buildAndSendEmail(to, testInstanceGroup, instanceId, null);
    }

}

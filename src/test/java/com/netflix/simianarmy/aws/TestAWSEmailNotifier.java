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

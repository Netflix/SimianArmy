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

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.netflix.simianarmy.MonkeyEmailNotifier;

/**
 * The class implements the monkey email notifier using AWS simple email service
 * for sending email.
 */
public abstract class AWSEmailNotifier implements MonkeyEmailNotifier {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AWSEmailNotifier.class);
    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+\\.#]+(.[_A-Za-z0-9-#]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private final Pattern emailPattern;

    private final AmazonSimpleEmailServiceClient sesClient;

    /**
     * The constructor.
     */
    public AWSEmailNotifier(AmazonSimpleEmailServiceClient sesClient) {
        super();
        this.sesClient = sesClient;
        this.emailPattern = Pattern.compile(EMAIL_PATTERN);
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        if (!isValidEmail(to)) {
            LOGGER.error(String.format("The destination email address %s is not valid, no email is sent.", to));
            return;
        }
        if (sesClient == null) {
            String msg = "The email client is not set.";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        Destination destination = new Destination().withToAddresses(to)
                .withCcAddresses(getCcAddresses(to));
        Content subjectContent = new Content(subject);
        Content bodyContent = new Content();
        Body msgBody = new Body(bodyContent);
        msgBody.setHtml(new Content(body));
        Message msg = new Message(subjectContent, msgBody);
        String sourceAddress = getSourceAddress(to);
        SendEmailRequest request = new SendEmailRequest(sourceAddress, destination, msg);
        request.setReturnPath(sourceAddress);
        LOGGER.debug(String.format("Sending email with subject '%s' to %s",
                subject, to));
        SendEmailResult result = null;
        try {
            result = sesClient.sendEmail(request);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to send email to %s", to), e);
        }
        LOGGER.info(String.format("Email to %s, result id is %s, subject is %s",
                to, result.getMessageId(), subject));
    }

    @Override
    public boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        if (!emailPattern.matcher(email).matches()) {
            LOGGER.error(String.format("Invalid email address: %s", email));
            return false;
        }
        if (email.equals("foo@bar.com")) {
            LOGGER.error(String.format("Email address not changed from default; treating as invalid: %s", email));
            return false;
        }
        return true;
    }
}

/*
 *
 *  Copyright 2013 Salesforce.com, Inc.
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import com.netflix.simianarmy.basic.BasicEmailClient;

/**
 * The class implements the EmailClient interface using AWS simple email service
 * for sending email.
 */
public class AWSEmailClient extends BasicEmailClient {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AWSEmailClient.class);

    private final AmazonSimpleEmailServiceClient sesClient;

    /**
     * The constructor.
     */
    public AWSEmailClient() {
        super(); //at this time, AWSEmailClient does not rely on any property configurations
        this.sesClient = new AmazonSimpleEmailServiceClient();
    }

    /* (non-Javadoc)
     * @see com.netflix.simianarmy.basic.BasicEmailClient#buildAndSendEmail(String, String, String[], String, String)
     */
    @Override
    protected String buildAndSendEmail(String to, String from, String[] cc, String subject, String body) {
        if (sesClient == null) {
            String msg = "The email client is not set.";
            LOGGER.error(msg);
            throw new RuntimeException(msg);
        }
        Destination destination = new Destination().withToAddresses(to)
                .withCcAddresses(cc);
        Content subjectContent = new Content(subject);
        Content bodyContent = new Content();
        Body msgBody = new Body(bodyContent);
        msgBody.setHtml(new Content(body));
        Message msg = new Message(subjectContent, msgBody);
        SendEmailRequest request = new SendEmailRequest(from, destination, msg);
        request.setReturnPath(from);
        LOGGER.debug(String.format("Sending email with subject '%s' to %s",
                subject, to));
        SendEmailResult result = sesClient.sendEmail(request);
        return result.getMessageId();
    }
}

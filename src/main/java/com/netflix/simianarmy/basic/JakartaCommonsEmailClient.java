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

package com.netflix.simianarmy.basic;


import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.basic.BasicEmailClient;

/**
 * The class implements the EmailClient interface using the Jakarta commons email library
 * to send email directly through an SMTP server.
 */
public class JakartaCommonsEmailClient extends BasicEmailClient {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JakartaCommonsEmailClient.class);
    /**
     * The constructor.
     */
    public JakartaCommonsEmailClient(MonkeyConfiguration cfg) {
        super(cfg);
    }
    
    /* (non-Javadoc)
     * @see com.netflix.simianarmy.basic.BasicEmailClient#buildAndSendEmail(String, String, String[], String, String)
     */
    @Override
    protected String buildAndSendEmail(String to, String from, String[] cc, String subject, String body) {
        Email email = new SimpleEmail();
        email.setHostName(cfg.getStrOrElse("simianarmy.client.smtp.host", "localhost"));
        email.setSmtpPort(Integer.parseInt(cfg.getStrOrElse("simianarmy.client.smtp.port", "25")));
        String smtpUser = cfg.getStr("simianarmy.client.smtp.username");
        String smtpPass = cfg.getStr("simianarmy.client.smtp.password");
        if(smtpUser != null && smtpPass != null){
          email.setAuthenticator(new DefaultAuthenticator(smtpUser, smtpPass));
        }
        email.setSubject(subject);
        
        String result = null;
        try {
            email.setFrom(from);
            email.setMsg(body);
            email.addTo(to);
            if(!ArrayUtils.isEmpty(cc)) email.addCc(cc);
            LOGGER.debug(String.format("Sending email with subject '%s' to %s",
                subject, to));
            result = email.send();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }

}

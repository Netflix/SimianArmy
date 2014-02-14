package com.netflix.simianarmy.basic;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.EmailClient;
import com.netflix.simianarmy.MonkeyConfiguration;

/**
 * This class provides basic functionality for delivering email.
 * Subclasses will implement the actual transport code, and optionally redefine the rules
 * of what makes an email address valid.
 * @author mgeis
 *
 */
public abstract class BasicEmailClient implements EmailClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicEmailClient.class);
    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    private static final Pattern emailPattern;
    private final MonkeyConfiguration cfg;

    static {
        emailPattern = Pattern.compile(EMAIL_PATTERN);
    }

    /**
     * The constructor.  Some transports requirea config to determine their connection parameters
     * (for example, connecting directly to an SMTP server).
     * @param cfg
     */
    public BasicEmailClient(MonkeyConfiguration cfg) {
        this.cfg = cfg;
    }

    /**
     * The constructor.  The no-arg constructor exists because some email transports are assumed
     * to be running in a service-oriented environment in which context is already available (for
     * example, AWS).
     */
    public BasicEmailClient() {
        this(null);
    }

    /**
     * Takes email content, initializes the necessary java classes, and delivers the mail.
     * This method takes care of the mail API internals.  The rest of the code that decorates the
     * message delivery (such as logging, exception handling) is in <code>sendEmail</code>.
     * Exceptions encountered should be either <code>RuntimeException</code> or wrapped in a
     * <code>RuntimeException</code>
     * @param to
     * @param from
     * @param cc
     * @param subject
     * @param body
     * @return The message id of the delivered message
     * @see #sendEmail(String, String, String[], String, String)
     */
    protected abstract String buildAndSendEmail(String to, String from, String[] cc, String subject, String body);

    /**
     * Checks email validity and raises exception if it does not pass the test.
     * @param address
     * @throws IllegalArgumentException if the email address provided is invalid
     */
    public void validateEmail(String address) {
        if (!isValidEmail(address)) {
            throw new IllegalArgumentException(String.format("Invalid email address: %s", address));
        }
    }

    /* (non-Javadoc)
     * @see com.netflix.simianarmy.EmailClient#sendEmail(String, String, String[], String, String)
     */
    @Override
    public void sendEmail(String to, String from, String[] cc, String subject, String body) {
        try {
            try {
                validateEmail(to);
            } catch (IllegalArgumentException iae) {
                LOGGER.error(String.format("The destination email address %s is not valid,  no email is sent.", to));
                return;
            }
            String messageId = buildAndSendEmail(to, from, cc, subject, body);
            LOGGER.info(String.format("Email to %s, result id is %s, subject is %s",
                to, messageId, subject));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to send email to %s", to), e);
        }
    }

    /* (non-Javadoc)
     * @see com.netflix.simianarmy.EmailClient#isValidEmail(String)
     */
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

    public MonkeyConfiguration getCfg() {
        return cfg;
    }

}

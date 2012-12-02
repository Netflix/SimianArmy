package com.netflix.simianarmy.chaos;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.netflix.simianarmy.aws.AWSEmailNotifier;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

/** The email notifier for Chaos monkey.
 *
 */
public abstract class ChaosEmailNotifier extends AWSEmailNotifier {

    /** Constructor. Currently the notifier is fixed the email client to
     * Amazon Simple Email Service. We can release this restriction when
     * we want to support different email client.
     *
     * @param sesClient the AWS simple email service client.
     */
    public ChaosEmailNotifier(AmazonSimpleEmailServiceClient sesClient) {
        super(sesClient);
    }

    /**
     * Sends an email notification for a termination of instance.
     * @param group the instance group
     * @param instance the instance id
     */
    public abstract void sendTerminationNotification(InstanceGroup group, String instance);

}

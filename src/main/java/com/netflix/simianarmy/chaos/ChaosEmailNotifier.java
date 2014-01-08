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
package com.netflix.simianarmy.chaos;

import java.lang.reflect.Constructor;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.simianarmy.EmailClient;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyEmailNotifier;
import com.netflix.simianarmy.aws.AWSEmailClient;
import com.netflix.simianarmy.basic.BasicConfiguration;
import com.netflix.simianarmy.chaos.ChaosCrawler.InstanceGroup;

/** The email notifier for Chaos monkey.
 *
 */
public abstract class ChaosEmailNotifier implements MonkeyEmailNotifier {
    protected EmailClient emailClient;
    private static final Logger LOGGER = LoggerFactory.getLogger(ChaosEmailNotifier.class);

    protected final MonkeyConfiguration cfg;
    
    /** Constructor. 
     *
     * @param cfg the monkey configuration used to initialize the email notifier.
     */
    public ChaosEmailNotifier(MonkeyConfiguration cfg) {
        this.cfg = cfg;
        createEmailClient();
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void createEmailClient() {
        BasicConfiguration config = (BasicConfiguration)cfg;
        Class emailClientClass = loadClientClass(config, "simianarmy.client.email.class");
        if (emailClientClass == null || emailClientClass.equals(AWSEmailClient.class)) {
            AWSEmailClient awsEmailClient = new AWSEmailClient();
            setEmailClient(awsEmailClient);
        } else {
            setEmailClient(( (EmailClient) factory(emailClientClass, config)));
        }
    }
    
    public EmailClient getEmailClient() {
        return emailClient;
    }

    public void setEmailClient(EmailClient emailClient) {
        this.emailClient = emailClient;
    }
    
    /**
     * Load a class specified by the config; for drop-in replacements.
     * (Duplicates a method in MonkeyServer; refactor to util?).
     *
     * @param config
     * @param key
     * @return The initialized class named in by the key, or null if empty or not found
     * @throws ServletException
     */
    @SuppressWarnings("rawtypes")
    protected Class loadClientClass(BasicConfiguration config, String key) {
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            String clientClassName = config.getStrOrElse(key, null);
            if (clientClassName == null || clientClassName.isEmpty()) {
                //LOGGER.info("using standard class for " + key);
                return null;
            }
        Class newClass = classLoader.loadClass(clientClassName);
            //LOGGER.info("using " + key + " loaded " + newClass.getCanonicalName());
            return newClass;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load " + key, e);
        }
    }

    /**
     * Generic factory to create monkey collateral types.
     *
     * @param <T>
     *            the generic type to create
     * @param implClass
     *            the actual concrete type to instantiate.
     * @return an object of the requested type
     */
    public <T> T factory(Class<T> implClass, BasicConfiguration config) {
        try {
            // then find corresponding ctor
            for (Constructor<?> ctor : implClass.getDeclaredConstructors()) {
                Class<?>[] paramTypes = ctor.getParameterTypes();
                if (paramTypes.length != 1) {
                    continue;
                }
                if (paramTypes[0].getName().endsWith("Configuration")) {
                    @SuppressWarnings("unchecked")
                    T impl = (T) ctor.newInstance(config);
                    return impl;
                }
            }
            // Last ditch; try no-arg.
            return implClass.newInstance();
        } catch (Exception e) {
            LOGGER.error("context config error, cannot make an instance of " + implClass.getName(), e);
        }
        return null;
    }

    /**
     * Sends an email notification for a termination of instance to group
     * owner's email address.
     * @param group the instance group
     * @param instance the instance id
     * @param chaosType the chosen chaos strategy
     */
    public abstract void sendTerminationNotification(InstanceGroup group, String instance, ChaosType chaosType);

    /**
     * Sends an email notification for a termination of instance to a global
     * email address.
     * @param group the instance group
     * @param instance the instance id
     * @param chaosType the chosen chaos strategy
     */
    public abstract void sendTerminationGlobalNotification(InstanceGroup group, String instance, ChaosType chaosType);

}

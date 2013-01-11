/*
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
// CHECKSTYLE IGNORE MagicNumberCheck
package com.netflix.simianarmy.basic.janitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.aws.janitor.ASGJanitor;
import com.netflix.simianarmy.aws.janitor.EBSSnapshotJanitor;
import com.netflix.simianarmy.aws.janitor.EBSVolumeJanitor;
import com.netflix.simianarmy.aws.janitor.InstanceJanitor;
import com.netflix.simianarmy.aws.janitor.SimpleDBJanitorResourceTracker;
import com.netflix.simianarmy.aws.janitor.crawler.ASGJanitorCrawler;
import com.netflix.simianarmy.aws.janitor.crawler.EBSSnapshotJanitorCrawler;
import com.netflix.simianarmy.aws.janitor.crawler.EBSVolumeJanitorCrawler;
import com.netflix.simianarmy.aws.janitor.crawler.InstanceJanitorCrawler;
import com.netflix.simianarmy.aws.janitor.rule.asg.OldEmptyASGRule;
import com.netflix.simianarmy.aws.janitor.rule.asg.SuspendedASGRule;
import com.netflix.simianarmy.aws.janitor.rule.instance.OrphanedInstanceRule;
import com.netflix.simianarmy.aws.janitor.rule.snapshot.NoGeneratedAMIRule;
import com.netflix.simianarmy.aws.janitor.rule.volume.OldDetachedVolumeRule;
import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import com.netflix.simianarmy.janitor.AbstractJanitor;
import com.netflix.simianarmy.janitor.JanitorCrawler;
import com.netflix.simianarmy.janitor.JanitorEmailBuilder;
import com.netflix.simianarmy.janitor.JanitorEmailNotifier;
import com.netflix.simianarmy.janitor.JanitorMonkey;
import com.netflix.simianarmy.janitor.JanitorResourceTracker;
import com.netflix.simianarmy.janitor.JanitorRuleEngine;

/**
 * The basic implementation of the context class for Janitor monkey.
 */
public class BasicJanitorMonkeyContext extends BasicSimianArmyContext implements JanitorMonkey.Context {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicJanitorMonkeyContext.class);

    /** The email notifier. */
    private final JanitorEmailNotifier emailNotifier;

    private final JanitorResourceTracker janitorResourceTracker;

    /** The janitors. */
    private final List<AbstractJanitor> janitors;

    private final String monkeyRegion;

    private final MonkeyCalendar monkeyCalendar;

    private final AmazonSimpleEmailServiceClient sesClient;

    private final JanitorEmailBuilder janitorEmailBuilder;

    private final String defaultEmail;

    private final String[] ccEmails;

    private final String sourceEmail;

    private final int daysBeforeTermination;

    /**
     * The constructor.
     */
    public BasicJanitorMonkeyContext() {
        super("simianarmy.properties", "client.properties", "janitor.properties");

        monkeyRegion = region();
        monkeyCalendar = calendar();

        String resourceDomain = configuration().getStrOrElse("simianarmy.janitor.resources.sdb.domain", "SIMIAN_ARMY");

        Set<String> enabledResourceSet = getEnabledResourceSet();

        janitorResourceTracker = new SimpleDBJanitorResourceTracker(awsClient(), resourceDomain);

        janitorEmailBuilder = new BasicJanitorEmailBuilder();
        sesClient = new AmazonSimpleEmailServiceClient();
        defaultEmail = configuration().getStrOrElse("simianarmy.janitor.notification.defaultEmail", "");
        ccEmails = StringUtils.split(
                configuration().getStrOrElse("simianarmy.janitor.notification.ccEmails", ""), ",");
        sourceEmail = configuration().getStrOrElse("simianarmy.janitor.notification.sourceEmail", "");
        daysBeforeTermination =
                (int) configuration().getNumOrElse("simianarmy.janitor.notification.daysBeforeTermination", 3);

        emailNotifier = new JanitorEmailNotifier(getJanitorEmailNotifierContext());

        janitors = new ArrayList<AbstractJanitor>();
        if (enabledResourceSet.contains("ASG")) {
            janitors.add(getASGJanitor());
        }

        if (enabledResourceSet.contains("INSTANCE")) {
            janitors.add(getInstanceJanitor());
        }

        if (enabledResourceSet.contains("EBS_VOLUME")) {
            janitors.add(getEBSVolumeJanitor());
        }

        if (enabledResourceSet.contains("EBS_SNAPSHOT")) {
            janitors.add(getEBSSnapshotJanitor());
        }
    }

    private ASGJanitor getASGJanitor() {
        JanitorRuleEngine ruleEngine = new BasicJanitorRuleEngine();
        boolean discoveryEnabled = configuration().getBoolOrElse("simianarmy.janitor.Eureka.enabled", false);
        DiscoveryClient discoveryClient;
        if (discoveryEnabled) {
            LOGGER.info("Initializing Discovery client.");
            discoveryClient = DiscoveryManager.getInstance().getDiscoveryClient();
        } else {
            LOGGER.info("Discovery/Eureka is not enabled.");
            discoveryClient = null;
        }
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.oldEmptyASGRule.enabled", false)) {
            ruleEngine.addRule(new OldEmptyASGRule(monkeyCalendar,
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.oldEmptyASGRule.launchConfigAgeThreshold", 50),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.oldEmptyASGRule.retentionDays", 10),
                                    discoveryClient
                    ));
        }

        if (configuration().getBoolOrElse("simianarmy.janitor.rule.suspendedASGRule.enabled", false)) {
            ruleEngine.addRule(new SuspendedASGRule(monkeyCalendar,
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.suspendedASGRule.suspensionAgeThreshold", 2),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.suspendedASGRule.retentionDays", 5),
                                    discoveryClient
                    ));
        }
        JanitorCrawler asgCrawler = new ASGJanitorCrawler(awsClient());
        BasicJanitorContext asgJanitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, asgCrawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new ASGJanitor(awsClient(), asgJanitorCtx);
    }

    private InstanceJanitor getInstanceJanitor() {
        JanitorRuleEngine ruleEngine = new BasicJanitorRuleEngine();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.orphanedInstanceRule.enabled", false)) {
            ruleEngine.addRule(new OrphanedInstanceRule(monkeyCalendar,
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.orphanedInstanceRule.instanceAgeThreshold", 2),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.orphanedInstanceRule.retentionDaysWithOwner", 3),
                                    (int) configuration().getNumOrElse(
                                            "simianarmy.janitor.rule.orphanedInstanceRule.retentionDaysWithoutOwner",
                                            8)));
        }
        JanitorCrawler instanceCrawler = new InstanceJanitorCrawler(awsClient());
        BasicJanitorContext instanceJanitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, instanceCrawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new InstanceJanitor(awsClient(), instanceJanitorCtx);
    }

    private EBSVolumeJanitor getEBSVolumeJanitor() {
        JanitorRuleEngine ruleEngine = new BasicJanitorRuleEngine();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.oldDetachedVolumeRule.enabled", false)) {
            ruleEngine.addRule(new OldDetachedVolumeRule(monkeyCalendar,
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.oldDetachedVolumeRule.detachDaysThreshold", 30),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.oldDetachedVolumeRule.retentionDays", 7)));
        }
        JanitorCrawler volumeCrawler = new EBSVolumeJanitorCrawler(awsClient());
        BasicJanitorContext volumeJanitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, volumeCrawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new EBSVolumeJanitor(awsClient(), volumeJanitorCtx);
    }

    private EBSSnapshotJanitor getEBSSnapshotJanitor() {
        JanitorRuleEngine ruleEngine = new BasicJanitorRuleEngine();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.noGeneratedAMIRule.enabled", false)) {
            ruleEngine.addRule(new NoGeneratedAMIRule(monkeyCalendar,
                    (int) configuration().getNumOrElse("simianarmy.janitor.rule.noGeneratedAMIRule.ageThreshold", 30),
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.noGeneratedAMIRule.retentionDays", 7)));
        }
        JanitorCrawler snapshotCrawler = new EBSSnapshotJanitorCrawler(awsClient());
        BasicJanitorContext snapshotJanitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, snapshotCrawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new EBSSnapshotJanitor(awsClient(), snapshotJanitorCtx);
    }

    private Set<String> getEnabledResourceSet() {
        Set<String> enabledResourceSet = new HashSet<String>();
        String enabledResources = configuration().getStr("simianarmy.janitor.enabledResources");
        if (StringUtils.isNotBlank(enabledResources)) {
            for (String resourceType : enabledResources.split(",")) {
                enabledResourceSet.add(resourceType.trim().toUpperCase());
            }
        }
        return enabledResourceSet;
    }

    public JanitorEmailNotifier.Context getJanitorEmailNotifierContext() {
        return new JanitorEmailNotifier.Context() {
            @Override
            public AmazonSimpleEmailServiceClient sesClient() {
                return sesClient;
            }

            @Override
            public String defaultEmail() {
                return defaultEmail;
            }

            @Override
            public int daysBeforeTermination() {
                return daysBeforeTermination;
            }

            @Override
            public String region() {
                return monkeyRegion;
            }

            @Override
            public JanitorResourceTracker resourceTracker() {
                return janitorResourceTracker;
            }

            @Override
            public JanitorEmailBuilder emailBuilder() {
                return janitorEmailBuilder;
            }

            @Override
            public MonkeyCalendar calendar() {
                return monkeyCalendar;
            }

            @Override
            public String[] ccEmails() {
                return ccEmails;
            }

            @Override
            public String sourceEmail() {
                return sourceEmail;
            }
        };
    }

    /** {@inheritDoc} */
    @Override
    public List<AbstractJanitor> janitors() {
        return janitors;
    }

    /** {@inheritDoc} */
    @Override
    public JanitorEmailNotifier emailNotifier() {
        return emailNotifier;
    }

    @Override
    public JanitorResourceTracker resourceTracker() {
        return janitorResourceTracker;
    }

    /** The Context class for Janitor.
     */
    public static class BasicJanitorContext implements AbstractJanitor.Context {
        private final String region;
        private final JanitorRuleEngine ruleEngine;
        private final JanitorCrawler crawler;
        private final JanitorResourceTracker resourceTracker;
        private final MonkeyCalendar calendar;
        private final MonkeyConfiguration config;
        private final MonkeyRecorder recorder;

        /**
         * Constructor.
         * @param region the region of the janitor
         * @param ruleEngine the rule engine used by the janitor
         * @param crawler the crawler used by the janitor
         * @param resourceTracker the resource tracker used by the janitor
         * @param calendar the calendar used by the janitor
         * @param config the monkey configuration used by the janitor
         */
        public BasicJanitorContext(String region, JanitorRuleEngine ruleEngine, JanitorCrawler crawler,
                JanitorResourceTracker resourceTracker, MonkeyCalendar calendar, MonkeyConfiguration config,
                MonkeyRecorder recorder) {
            this.region = region;
            this.resourceTracker = resourceTracker;
            this.ruleEngine = ruleEngine;
            this.crawler = crawler;
            this.calendar = calendar;
            this.config = config;
            this.recorder = recorder;
        }

        @Override
        public String region() {
            return region;
        }

        @Override
        public MonkeyConfiguration configuration() {
            return config;
        }

        @Override
        public MonkeyCalendar calendar() {
            return calendar;
        }

        @Override
        public JanitorRuleEngine janitorRuleEngine() {
            return ruleEngine;
        }

        @Override
        public JanitorCrawler janitorCrawler() {
            return crawler;
        }

        @Override
        public JanitorResourceTracker janitorResourceTracker() {
            return resourceTracker;
        }

        @Override
        public MonkeyRecorder recorder() {
            return recorder;
        }
    }
}

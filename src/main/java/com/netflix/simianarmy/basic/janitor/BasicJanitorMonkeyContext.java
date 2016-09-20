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

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.guice.EurekaModule;
import com.netflix.simianarmy.MonkeyCalendar;
import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.MonkeyRecorder;
import com.netflix.simianarmy.aws.janitor.*;
import com.netflix.simianarmy.aws.janitor.crawler.*;
import com.netflix.simianarmy.aws.janitor.crawler.edda.*;
import com.netflix.simianarmy.aws.janitor.rule.ami.UnusedImageRule;
import com.netflix.simianarmy.aws.janitor.rule.asg.*;
import com.netflix.simianarmy.aws.janitor.rule.elb.OrphanedELBRule;
import com.netflix.simianarmy.aws.janitor.rule.generic.TagValueExclusionRule;
import com.netflix.simianarmy.aws.janitor.rule.generic.UntaggedRule;
import com.netflix.simianarmy.aws.janitor.rule.instance.OrphanedInstanceRule;
import com.netflix.simianarmy.aws.janitor.rule.launchconfig.OldUnusedLaunchConfigRule;
import com.netflix.simianarmy.aws.janitor.rule.snapshot.NoGeneratedAMIRule;
import com.netflix.simianarmy.aws.janitor.rule.volume.DeleteOnTerminationRule;
import com.netflix.simianarmy.aws.janitor.rule.volume.OldDetachedVolumeRule;
import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import com.netflix.simianarmy.client.edda.EddaClient;
import com.netflix.simianarmy.janitor.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private final String ownerEmailDomain;

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

        String dbDriver = configuration().getStr("simianarmy.recorder.db.driver");
        String dbUser = configuration().getStr("simianarmy.recorder.db.user");
        String dbPass = configuration().getStr("simianarmy.recorder.db.pass");
        String dbUrl = configuration().getStr("simianarmy.recorder.db.url");
        String dbTable = configuration().getStr("simianarmy.janitor.resources.db.table");
        
        if (dbDriver == null) {       
        	janitorResourceTracker = new SimpleDBJanitorResourceTracker(awsClient(), resourceDomain);
        } else {
        	RDSJanitorResourceTracker rdsTracker = new RDSJanitorResourceTracker(dbDriver, dbUser, dbPass, dbUrl, dbTable);
        	rdsTracker.init();
        	janitorResourceTracker = rdsTracker;
        }

        janitorEmailBuilder = new BasicJanitorEmailBuilder();
        sesClient = new AmazonSimpleEmailServiceClient();
        if (configuration().getStr("simianarmy.aws.email.region") != null) {
           sesClient.setRegion(Region.getRegion(Regions.fromName(configuration().getStr("simianarmy.aws.email.region"))));
        }
        defaultEmail = configuration().getStrOrElse("simianarmy.janitor.notification.defaultEmail", "");
        ccEmails = StringUtils.split(
                configuration().getStrOrElse("simianarmy.janitor.notification.ccEmails", ""), ",");
        sourceEmail = configuration().getStrOrElse("simianarmy.janitor.notification.sourceEmail", "");
        ownerEmailDomain = configuration().getStrOrElse("simianarmy.janitor.notification.ownerEmailDomain", "");
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

        if (enabledResourceSet.contains("LAUNCH_CONFIG")) {
            janitors.add(getLaunchConfigJanitor());
        }

        if (enabledResourceSet.contains("IMAGE")) {
            janitors.add(getImageJanitor());
        }

        if (enabledResourceSet.contains("ELB")) {
            janitors.add(getELBJanitor());
        }

    }
    protected JanitorRuleEngine createJanitorRuleEngine() {
        JanitorRuleEngine ruleEngine = new BasicJanitorRuleEngine();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.TagValueExclusionRule.enabled", false)) {
            String tagsList = configuration().getStr("simianarmy.janitor.rule.TagValueExclusionRule.tags");
            String valsList = configuration().getStr("simianarmy.janitor.rule.TagValueExclusionRule.vals");
            if (tagsList != null && valsList != null) {
                TagValueExclusionRule rule = new TagValueExclusionRule(tagsList.split(","), valsList.split(","));
                ruleEngine.addExclusionRule(rule);
            }
        }
        return ruleEngine;
    }

    private ASGJanitor getASGJanitor() {
        JanitorRuleEngine ruleEngine = createJanitorRuleEngine();
        boolean discoveryEnabled = configuration().getBoolOrElse("simianarmy.janitor.Eureka.enabled", false);
        ASGInstanceValidator instanceValidator;
        if (discoveryEnabled) {
            LOGGER.info("Initializing Discovery client.");
            Injector injector = Guice.createInjector(new EurekaModule());
            DiscoveryClient discoveryClient = injector.getInstance(DiscoveryClient.class);
            instanceValidator = new DiscoveryASGInstanceValidator(discoveryClient);
        } else {
            LOGGER.info("Discovery/Eureka is not enabled, use the dummy instance validator.");
            instanceValidator = new DummyASGInstanceValidator();
        }
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.oldEmptyASGRule.enabled", false)) {
            ruleEngine.addRule(new OldEmptyASGRule(monkeyCalendar,
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.oldEmptyASGRule.launchConfigAgeThreshold", 50),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.oldEmptyASGRule.retentionDays", 10),
                                    instanceValidator
                    ));
        }

        if (configuration().getBoolOrElse("simianarmy.janitor.rule.suspendedASGRule.enabled", false)) {
            ruleEngine.addRule(new SuspendedASGRule(monkeyCalendar,
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.suspendedASGRule.suspensionAgeThreshold", 2),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.suspendedASGRule.retentionDays", 5),
                                    instanceValidator
                    ));
        }
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.untaggedRule.enabled", false)
            && getUntaggedRuleResourceSet().contains("ASG")) {
            ruleEngine.addRule(new UntaggedRule(monkeyCalendar, getPropertySet("simianarmy.janitor.rule.untaggedRule.requiredTags"),
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.untaggedRule.retentionDaysWithOwner", 3),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.untaggedRule.retentionDaysWithoutOwner",
                                    8)));
        }

        JanitorCrawler crawler;
        if (configuration().getBoolOrElse("simianarmy.janitor.edda.enabled", false)) {
            crawler = new EddaASGJanitorCrawler(createEddaClient(), awsClient().region());
        } else {
            crawler = new ASGJanitorCrawler(awsClient());
        }
        BasicJanitorContext asgJanitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, crawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new ASGJanitor(awsClient(), asgJanitorCtx);
    }

    private InstanceJanitor getInstanceJanitor() {
        JanitorRuleEngine ruleEngine = createJanitorRuleEngine();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.orphanedInstanceRule.enabled", false)) {
            ruleEngine.addRule(new OrphanedInstanceRule(monkeyCalendar,
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.orphanedInstanceRule.instanceAgeThreshold", 2),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.orphanedInstanceRule.retentionDaysWithOwner", 3),
                                    (int) configuration().getNumOrElse(
                                            "simianarmy.janitor.rule.orphanedInstanceRule.retentionDaysWithoutOwner",
                                            8),
                                    configuration().getBoolOrElse(
                                            "simianarmy.janitor.rule.orphanedInstanceRule.opsworks.parentage",
                                            false)));
        }
        
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.untaggedRule.enabled", false)
            && getUntaggedRuleResourceSet().contains("INSTANCE")) {
            ruleEngine.addRule(new UntaggedRule(monkeyCalendar, getPropertySet("simianarmy.janitor.rule.untaggedRule.requiredTags"),
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.untaggedRule.retentionDaysWithOwner", 3),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.untaggedRule.retentionDaysWithoutOwner",
                                    8)));
        }

        JanitorCrawler instanceCrawler;
        if (configuration().getBoolOrElse("simianarmy.janitor.edda.enabled", false)) {
            instanceCrawler = new EddaInstanceJanitorCrawler(createEddaClient(), awsClient().region());
        } else {
            instanceCrawler = new InstanceJanitorCrawler(awsClient());
        }
        BasicJanitorContext instanceJanitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, instanceCrawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new InstanceJanitor(awsClient(), instanceJanitorCtx);
    }

    private EBSVolumeJanitor getEBSVolumeJanitor() {
        JanitorRuleEngine ruleEngine = createJanitorRuleEngine();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.oldDetachedVolumeRule.enabled", false)) {
            ruleEngine.addRule(new OldDetachedVolumeRule(monkeyCalendar,
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.oldDetachedVolumeRule.detachDaysThreshold", 30),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.oldDetachedVolumeRule.retentionDays", 7)));

            if (configuration().getBoolOrElse("simianarmy.janitor.edda.enabled", false)
                && configuration().getBoolOrElse("simianarmy.janitor.rule.deleteOnTerminationRule.enabled", false)) {
                ruleEngine.addRule(new DeleteOnTerminationRule(monkeyCalendar, (int) configuration().getNumOrElse(
                        "simianarmy.janitor.rule.deleteOnTerminationRule.retentionDays", 3)));
            }
        }
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.untaggedRule.enabled", false)
            && getUntaggedRuleResourceSet().contains("EBS_VOLUME")) {
            ruleEngine.addRule(new UntaggedRule(monkeyCalendar, getPropertySet("simianarmy.janitor.rule.untaggedRule.requiredTags"),
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.untaggedRule.retentionDaysWithOwner", 3),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.untaggedRule.retentionDaysWithoutOwner",
                                    8)));
        }

        JanitorCrawler volumeCrawler;
        if (configuration().getBoolOrElse("simianarmy.janitor.edda.enabled", false)) {
            volumeCrawler = new EddaEBSVolumeJanitorCrawler(createEddaClient(), awsClient().region());
        } else {
            volumeCrawler = new EBSVolumeJanitorCrawler(awsClient());
        }

        BasicJanitorContext volumeJanitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, volumeCrawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new EBSVolumeJanitor(awsClient(), volumeJanitorCtx);
    }

    private EBSSnapshotJanitor getEBSSnapshotJanitor() {
        JanitorRuleEngine ruleEngine = createJanitorRuleEngine();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.noGeneratedAMIRule.enabled", false)) {
            ruleEngine.addRule(new NoGeneratedAMIRule(monkeyCalendar,
                    (int) configuration().getNumOrElse("simianarmy.janitor.rule.noGeneratedAMIRule.ageThreshold", 30),
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.noGeneratedAMIRule.retentionDays", 7),
                    configuration().getStrOrElse(
                            "simianarmy.janitor.rule.noGeneratedAMIRule.ownerEmail", null)));
        }
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.untaggedRule.enabled", false)
            && getUntaggedRuleResourceSet().contains("EBS_SNAPSHOT")) {
            ruleEngine.addRule(new UntaggedRule(monkeyCalendar, getPropertySet("simianarmy.janitor.rule.untaggedRule.requiredTags"),
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.untaggedRule.retentionDaysWithOwner", 3),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.untaggedRule.retentionDaysWithoutOwner",
                                    8)));
        }

        JanitorCrawler snapshotCrawler;
        if (configuration().getBoolOrElse("simianarmy.janitor.edda.enabled", false)) {
            snapshotCrawler = new EddaEBSSnapshotJanitorCrawler(
                    configuration().getStr("simianarmy.janitor.snapshots.ownerId"),
                    createEddaClient(), awsClient().region());
        } else {
            snapshotCrawler = new EBSSnapshotJanitorCrawler(awsClient());
        }
        BasicJanitorContext snapshotJanitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, snapshotCrawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new EBSSnapshotJanitor(awsClient(), snapshotJanitorCtx);
    }

    private LaunchConfigJanitor getLaunchConfigJanitor() {
        JanitorRuleEngine ruleEngine = createJanitorRuleEngine();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.oldUnusedLaunchConfigRule.enabled", false)) {
            ruleEngine.addRule(new OldUnusedLaunchConfigRule(monkeyCalendar,
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.oldUnusedLaunchConfigRule.ageThreshold", 4),
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.oldUnusedLaunchConfigRule.retentionDays", 3)));
        }
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.untaggedRule.enabled", false)
            && getUntaggedRuleResourceSet().contains("LAUNCH_CONFIG")) {
            ruleEngine.addRule(new UntaggedRule(monkeyCalendar, getPropertySet("simianarmy.janitor.rule.untaggedRule.requiredTags"),
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.untaggedRule.retentionDaysWithOwner", 3),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.untaggedRule.retentionDaysWithoutOwner",
                                    8)));
        }

        JanitorCrawler crawler;
        if (configuration().getBoolOrElse("simianarmy.janitor.edda.enabled", false)) {
            crawler = new EddaLaunchConfigJanitorCrawler(
                    createEddaClient(), awsClient().region());
        } else {
            crawler = new LaunchConfigJanitorCrawler(awsClient());
        }
        BasicJanitorContext janitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, crawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new LaunchConfigJanitor(awsClient(), janitorCtx);
    }

    private ImageJanitor getImageJanitor() {
        JanitorCrawler crawler;
        if (configuration().getBoolOrElse("simianarmy.janitor.edda.enabled", false)) {
            crawler = new EddaImageJanitorCrawler(createEddaClient(),
                    configuration().getStr("simianarmy.janitor.image.ownerId"),
                    (int) configuration().getNumOrElse("simianarmy.janitor.image.crawler.lookBackDays", 60),
                    awsClient().region());
        } else {
            throw new RuntimeException("Image Janitor only works when Edda is enabled.");
        }

        JanitorRuleEngine ruleEngine = createJanitorRuleEngine();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.unusedImageRule.enabled", false)) {
            ruleEngine.addRule(new UnusedImageRule(monkeyCalendar,
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.unusedImageRule.retentionDays", 3),
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.unusedImageRule.lastReferenceDaysThreshold", 45)));
        }
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.untaggedRule.enabled", false)
            && getUntaggedRuleResourceSet().contains("IMAGE")) {
            ruleEngine.addRule(new UntaggedRule(monkeyCalendar, getPropertySet("simianarmy.janitor.rule.untaggedRule.requiredTags"),
                    (int) configuration().getNumOrElse(
                            "simianarmy.janitor.rule.untaggedRule.retentionDaysWithOwner", 3),
                            (int) configuration().getNumOrElse(
                                    "simianarmy.janitor.rule.untaggedRule.retentionDaysWithoutOwner",
                                    8)));
        }

        BasicJanitorContext janitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, crawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new ImageJanitor(awsClient(), janitorCtx);
    }


    private ELBJanitor getELBJanitor() {
        JanitorRuleEngine ruleEngine = createJanitorRuleEngine();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.orphanedELBRule.enabled", false)) {
            ruleEngine.addRule(new OrphanedELBRule(monkeyCalendar,
                (int) configuration().getNumOrElse(
                        "simianarmy.janitor.rule.orphanedELBRule.retentionDays", 7)));
        }

        JanitorCrawler elbCrawler;
        if (configuration().getBoolOrElse("simianarmy.janitor.edda.enabled", false)) {
            boolean useEddaApplicationOwner = configuration().getBoolOrElse("simianarmy.janitor.rule.orphanedELBRule.edda.useApplicationOwner", false);
            String eddaFallbackOwnerEmail = configuration().getStr("simianarmy.janitor.rule.orphanedELBRule.edda.fallbackOwnerEmail");
            elbCrawler = new EddaELBJanitorCrawler(createEddaClient(), eddaFallbackOwnerEmail, useEddaApplicationOwner, awsClient().region());
        } else {
            elbCrawler = new ELBJanitorCrawler(awsClient());
        }
        BasicJanitorContext elbJanitorCtx = new BasicJanitorContext(
                monkeyRegion, ruleEngine, elbCrawler, janitorResourceTracker,
                monkeyCalendar, configuration(), recorder());
        return new ELBJanitor(awsClient(), elbJanitorCtx);
    }

    private EddaClient createEddaClient() {
        return new EddaClient((int) configuration().getNumOrElse("simianarmy.janitor.edda.client.timeout", 30000),
                (int) configuration().getNumOrElse("simianarmy.janitor.edda.client.retries", 3),
                (int) configuration().getNumOrElse("simianarmy.janitor.edda.client.retryInterval", 1000),
                configuration());
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

    private Set<String> getUntaggedRuleResourceSet() {
        Set<String> untaggedRuleResourceSet = new HashSet<String>();
        if (configuration().getBoolOrElse("simianarmy.janitor.rule.untaggedRule.enabled", false)) {
            String untaggedRuleResources = configuration().getStr("simianarmy.janitor.rule.untaggedRule.resources");
            if (StringUtils.isNotBlank(untaggedRuleResources)) {
                for (String resourceType : untaggedRuleResources.split(",")) {
                    untaggedRuleResourceSet.add(resourceType.trim().toUpperCase());
                }
            }
        }
        return untaggedRuleResourceSet;
    }
 
    private Set<String> getPropertySet(String property) {
        Set<String> propertyValueSet = new HashSet<String>();
        String propertyValue = configuration().getStr(property);
        if (StringUtils.isNotBlank(propertyValue)) {
            for (String propertyValueItem : propertyValue.split(",")) {
                propertyValueSet.add(propertyValueItem.trim());
            }
        }
        return propertyValueSet;
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

            @Override
            public String ownerEmailDomain() {
                return ownerEmailDomain;
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

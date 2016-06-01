/*
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
// CHECKSTYLE IGNORE MagicNumberCheck
package com.netflix.simianarmy.basic.conformity;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.guice.EurekaModule;
import com.netflix.simianarmy.aws.conformity.RDSConformityClusterTracker;
import com.netflix.simianarmy.aws.conformity.SimpleDBConformityClusterTracker;
import com.netflix.simianarmy.aws.conformity.crawler.AWSClusterCrawler;
import com.netflix.simianarmy.aws.conformity.rule.BasicConformityEurekaClient;
import com.netflix.simianarmy.aws.conformity.rule.ConformityEurekaClient;
import com.netflix.simianarmy.aws.conformity.rule.CrossZoneLoadBalancing;
import com.netflix.simianarmy.aws.conformity.rule.InstanceHasHealthCheckUrl;
import com.netflix.simianarmy.aws.conformity.rule.InstanceHasStatusUrl;
import com.netflix.simianarmy.aws.conformity.rule.InstanceInSecurityGroup;
import com.netflix.simianarmy.aws.conformity.rule.InstanceInVPC;
import com.netflix.simianarmy.aws.conformity.rule.InstanceIsHealthyInEureka;
import com.netflix.simianarmy.aws.conformity.rule.InstanceTooOld;
import com.netflix.simianarmy.aws.conformity.rule.SameZonesInElbAndAsg;
import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.conformity.ClusterCrawler;
import com.netflix.simianarmy.conformity.ConformityClusterTracker;
import com.netflix.simianarmy.conformity.ConformityEmailBuilder;
import com.netflix.simianarmy.conformity.ConformityEmailNotifier;
import com.netflix.simianarmy.conformity.ConformityMonkey;
import com.netflix.simianarmy.conformity.ConformityRule;
import com.netflix.simianarmy.conformity.ConformityRuleEngine;

/**
 * The basic implementation of the context class for Conformity monkey.
 */
public class BasicConformityMonkeyContext extends BasicSimianArmyContext implements ConformityMonkey.Context {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicConformityMonkeyContext.class);

    /** The email notifier. */
    private final ConformityEmailNotifier emailNotifier;

    private final ConformityClusterTracker clusterTracker;

    private final Collection<String> regions;

    private final ClusterCrawler clusterCrawler;

    private final AmazonSimpleEmailServiceClient sesClient;

    private final ConformityEmailBuilder conformityEmailBuilder;

    private final String defaultEmail;

    private final String[] ccEmails;

    private final String sourceEmail;

    private final ConformityRuleEngine ruleEngine;

    private final boolean leashed;

    private final Map<String, AWSClient> regionToAwsClient = Maps.newHashMap();

    /**
     * The constructor.
     */
    public BasicConformityMonkeyContext() {
        super("simianarmy.properties", "client.properties", "conformity.properties");
        regions = Lists.newArrayList(region());

        // By default, the monkey is leashed
        leashed = configuration().getBoolOrElse("simianarmy.conformity.leashed", true);

        LOGGER.info(String.format("Conformity Monkey is running in: %s", regions));

        String sdbDomain = configuration().getStrOrElse("simianarmy.conformity.sdb.domain", "SIMIAN_ARMY");

        String dbDriver = configuration().getStr("simianarmy.recorder.db.driver");
        String dbUser = configuration().getStr("simianarmy.recorder.db.user");
        String dbPass = configuration().getStr("simianarmy.recorder.db.pass");
        String dbUrl = configuration().getStr("simianarmy.recorder.db.url");
        String dbTable = configuration().getStr("simianarmy.conformity.resources.db.table");
        
        if (dbDriver == null) {       
        	clusterTracker = new SimpleDBConformityClusterTracker(awsClient(), sdbDomain);
        } else {
        	RDSConformityClusterTracker rdsClusterTracker = new RDSConformityClusterTracker(dbDriver, dbUser, dbPass, dbUrl, dbTable);
        	rdsClusterTracker.init();
        	clusterTracker = rdsClusterTracker;
        }

        ruleEngine = new ConformityRuleEngine();
        boolean eurekaEnabled = configuration().getBoolOrElse("simianarmy.conformity.Eureka.enabled", false);

        if (eurekaEnabled) {
            LOGGER.info("Initializing Discovery client.");
            Injector injector = Guice.createInjector(new EurekaModule());
            DiscoveryClient discoveryClient = injector.getInstance(DiscoveryClient.class);
            ConformityEurekaClient conformityEurekaClient = new BasicConformityEurekaClient(discoveryClient);
            if (configuration().getBoolOrElse(
                    "simianarmy.conformity.rule.InstanceIsHealthyInEureka.enabled", false)) {
                ruleEngine.addRule(new InstanceIsHealthyInEureka(conformityEurekaClient));
            }
            if (configuration().getBoolOrElse(
                    "simianarmy.conformity.rule.InstanceHasHealthCheckUrl.enabled", false)) {
                ruleEngine.addRule(new InstanceHasHealthCheckUrl(conformityEurekaClient));
            }
            if (configuration().getBoolOrElse(
                    "simianarmy.conformity.rule.InstanceHasStatusUrl.enabled", false)) {
                ruleEngine.addRule(new InstanceHasStatusUrl(conformityEurekaClient));
            }
        } else {
            LOGGER.info("Discovery/Eureka is not enabled, the conformity rules that need Eureka are not added.");
        }

        if (configuration().getBoolOrElse(
                "simianarmy.conformity.rule.InstanceInSecurityGroup.enabled", false)) {
            String requiredSecurityGroups = configuration().getStr(
                    "simianarmy.conformity.rule.InstanceInSecurityGroup.requiredSecurityGroups");
            if (!StringUtils.isBlank(requiredSecurityGroups)) {
                ruleEngine.addRule(new InstanceInSecurityGroup(getAwsCredentialsProvider(),
                        StringUtils.split(requiredSecurityGroups, ",")));
            } else {
                LOGGER.info("No required security groups is specified, "
                        + "the conformity rule InstanceInSecurityGroup is ignored.");
            }
        }

        if (configuration().getBoolOrElse(
                "simianarmy.conformity.rule.InstanceTooOld.enabled", false)) {
                ruleEngine.addRule(new InstanceTooOld(getAwsCredentialsProvider(), (int) configuration().getNumOrElse(
                        "simianarmy.conformity.rule.InstanceTooOld.instanceAgeThreshold", 180)));
        }

        if (configuration().getBoolOrElse(
                "simianarmy.conformity.rule.SameZonesInElbAndAsg.enabled", false)) {
            ruleEngine().addRule(new SameZonesInElbAndAsg(getAwsCredentialsProvider()));
        }

        if (configuration().getBoolOrElse(
                "simianarmy.conformity.rule.InstanceInVPC.enabled", false)) {
                ruleEngine.addRule(new InstanceInVPC(getAwsCredentialsProvider()));
        }

        if (configuration().getBoolOrElse(
                "simianarmy.conformity.rule.CrossZoneLoadBalancing.enabled", false)) {
                ruleEngine().addRule(new CrossZoneLoadBalancing(getAwsCredentialsProvider()));
        }
        
        createClient(region());
        regionToAwsClient.put(region(), awsClient());

        clusterCrawler = new AWSClusterCrawler(regionToAwsClient, configuration());
        sesClient = new AmazonSimpleEmailServiceClient();
        if (configuration().getStr("simianarmy.aws.email.region") != null) {
          sesClient.setRegion(Region.getRegion(Regions.fromName(configuration().getStr("simianarmy.aws.email.region"))));
        }        
        defaultEmail = configuration().getStrOrElse("simianarmy.conformity.notification.defaultEmail", null);
        ccEmails = StringUtils.split(
                configuration().getStrOrElse("simianarmy.conformity.notification.ccEmails", ""), ",");
        sourceEmail = configuration().getStrOrElse("simianarmy.conformity.notification.sourceEmail", null);
        conformityEmailBuilder = new BasicConformityEmailBuilder();
        emailNotifier = new ConformityEmailNotifier(getConformityEmailNotifierContext());
    }

    public ConformityEmailNotifier.Context getConformityEmailNotifierContext() {
        return new ConformityEmailNotifier.Context() {
            @Override
            public AmazonSimpleEmailServiceClient sesClient() {
                return sesClient;
            }

            @Override
            public int openHour() {
                return (int) configuration().getNumOrElse("simianarmy.conformity.notification.openHour", 0);
            }

            @Override
            public int closeHour() {
                return (int) configuration().getNumOrElse("simianarmy.conformity.notification.closeHour", 24);
            }

            @Override
            public String defaultEmail() {
                return defaultEmail;
            }

            @Override
            public Collection<String> regions() {
                return regions;
            }

            @Override
            public ConformityClusterTracker clusterTracker() {
                return clusterTracker;
            }

            @Override
            public ConformityEmailBuilder emailBuilder() {
                return conformityEmailBuilder;
            }

            @Override
            public String[] ccEmails() {
                return ccEmails;
            }

            @Override
            public Collection<ConformityRule> rules() {
                return ruleEngine.rules();
            }

            @Override
            public String sourceEmail() {
                return sourceEmail;
            }
        };
    }

    @Override
    public ClusterCrawler clusterCrawler() {
        return clusterCrawler;
    }

    @Override
    public ConformityRuleEngine ruleEngine() {
        return ruleEngine;
    }

    /** {@inheritDoc} */
    @Override
    public ConformityEmailNotifier emailNotifier() {
        return emailNotifier;
    }

    @Override
    public Collection<String> regions() {
        return regions;
    }

    @Override
    public boolean isLeashed() {
        return leashed;
    }

    @Override
    public ConformityClusterTracker clusterTracker() {
        return clusterTracker;
    }
}

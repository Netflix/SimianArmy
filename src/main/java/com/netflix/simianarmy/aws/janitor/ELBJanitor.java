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

package com.netflix.simianarmy.aws.janitor;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.client.aws.AWSClient;
import com.netflix.simianarmy.janitor.AbstractJanitor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Janitor responsible for elastic load balancer cleanup.
 */
public class ELBJanitor extends AbstractJanitor {

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ELBJanitor.class);

    private final AWSClient awsClient;

    /**
     * Constructor.
     * @param awsClient the AWS client
     * @param ctx the context
     */
    public ELBJanitor(AWSClient awsClient, Context ctx) {
        super(ctx, AWSResourceType.ELB);
        Validate.notNull(awsClient);
        this.awsClient = awsClient;
    }

    @Override
    protected void postMark(Resource resource) {
    }

    @Override
    protected void cleanup(Resource resource) {
        LOGGER.info(String.format("Deleting ELB %s", resource.getId()));
        awsClient.deleteElasticLoadBalancer(resource.getId());

        // delete any DNS records attached to this ELB
        String dnsNames = resource.getAdditionalField("referencedDNS");
        String dnsTypes = resource.getAdditionalField("referencedDNSTypes");
        String dnsZones = resource.getAdditionalField("referencedDNSZones");
        if (StringUtils.isNotBlank(dnsNames) && StringUtils.isNotBlank(dnsTypes) && StringUtils.isNotBlank(dnsZones)) {
            String[] dnsNamesSplit = StringUtils.split(dnsNames,',');
            String[] dnsTypesSplit = StringUtils.split(dnsTypes,',');
            String[] dnsZonesSplit = StringUtils.split(dnsZones,',');

            if (dnsNamesSplit.length != dnsTypesSplit.length) {
                LOGGER.error(String.format("DNS Name count does not match DNS Type count, aborting DNS delete for ELB %s"), resource.getId());
                LOGGER.error(String.format("DNS Names found but not deleted: %s for ELB %s"), dnsNames, resource.getId());
                return;
            }

            if (dnsNamesSplit.length != dnsZonesSplit.length) {
                LOGGER.error(String.format("DNS Name count does not match DNS Zone count, aborting DNS delete for ELB %s"), resource.getId());
                LOGGER.error(String.format("DNS Names found but not deleted: %s for ELB %s"), dnsNames, resource.getId());
                return;
            }

            for(int i=0; i<dnsNamesSplit.length; i++) {
                LOGGER.info(String.format("Deleting DNS Record %s for ELB %s of type %s in zone %s", dnsNamesSplit[i], resource.getId(), dnsTypesSplit[i], dnsZonesSplit[i]));
                awsClient.deleteDNSRecord(dnsNamesSplit[i], dnsTypesSplit[i], dnsZonesSplit[i]);
            }
        }
    }

    @Override
    protected void postCleanup(Resource resource) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            LOGGER.warn("Post-cleanup sleep was interrupted", e);
        }
    }

}

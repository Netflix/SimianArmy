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

package com.netflix.simianarmy.aws.janitor.crawler.edda;

import com.google.common.collect.Lists;
import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.ResourceType;
import com.netflix.simianarmy.aws.AWSResource;
import com.netflix.simianarmy.aws.AWSResourceType;
import com.netflix.simianarmy.basic.BasicSimianArmyContext;
import com.netflix.simianarmy.client.edda.EddaClient;
import com.netflix.simianarmy.janitor.JanitorCrawler;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * The crawler to crawl AWS instances for janitor monkey using Edda.
 */
public class EddaELBJanitorCrawler implements JanitorCrawler {

    class DNSEntry {
        String dnsName;
        String dnsType;
        String hostedZoneId;
    };

    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EddaELBJanitorCrawler.class);

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

    private final EddaClient eddaClient;
    private final List<String> regions = Lists.newArrayList();
    private final boolean useEddaApplicationOwner;
    private final String fallbackOwnerEmail;

    private Map<String, String> applicationToOwner = new HashMap<String, String>();

    /**
     * Instantiates a new basic instance crawler.
     * @param eddaClient
     *            the Edda client
     * @param regions
     *            the regions the crawler will crawl resources for
     */
    public EddaELBJanitorCrawler(EddaClient eddaClient, String fallbackOwnerEmail, boolean useEddaApplicationOwner, String... regions) {
        this.useEddaApplicationOwner = useEddaApplicationOwner;
        this.fallbackOwnerEmail = fallbackOwnerEmail;
        Validate.notNull(eddaClient);
        this.eddaClient = eddaClient;
        Validate.notNull(regions);
        for (String region : regions) {
            this.regions.add(region);
        }
    }

    @Override
    public EnumSet<? extends ResourceType> resourceTypes() {
        return EnumSet.of(AWSResourceType.ELB);
    }

    @Override
    public List<Resource> resources(ResourceType resourceType) {
        if ("ELB".equals(resourceType.name())) {
            return getELBResources();
        }
        return Collections.emptyList();
    }

    @Override
    public List<Resource> resources(String... resourceIds) {
        return getELBResources(resourceIds);
    }

    @Override
    public String getOwnerEmailForResource(Resource resource) {
        Validate.notNull(resource);
        String ownerEmail = null;
        if (useEddaApplicationOwner) {
            for (String app : applicationToOwner.keySet()) {
                if (resource.getId().toLowerCase().startsWith(app)) {
                    ownerEmail = applicationToOwner.get(app);
                    break;
                }
            }
        } else {
            ownerEmail = resource.getTag(BasicSimianArmyContext.GLOBAL_OWNER_TAGKEY);
        }

        if (ownerEmail == null) {
            ownerEmail = fallbackOwnerEmail;
        }

        return ownerEmail;
    }

    private List<Resource> getELBResources(String... instanceIds) {
        if (useEddaApplicationOwner) {
            applicationToOwner = EddaUtils.getAllApplicationOwnerEmails(eddaClient);
        }

        List<Resource> resources = Lists.newArrayList();
        for (String region : regions) {
            resources.addAll(getELBResourcesInRegion(region, instanceIds));
        }
        return resources;
    }

    private List<Resource> getELBResourcesInRegion(String region, String... elbNames) {
        String url = eddaClient.getBaseUrl(region) + "/aws/loadBalancers";
        if (elbNames != null && elbNames.length != 0) {
            url += StringUtils.join(elbNames, ',');
            LOGGER.info(String.format("Getting ELBs in region %s for %d names", region, elbNames.length));
        } else {
            LOGGER.info(String.format("Getting all ELBs in region %s", region));
        }

        url += ";_expand:(loadBalancerName,createdTime,DNSName,instances,tags:(key,value))";

        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get Jason node from edda for ELBs in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }


        List<Resource> resources = Lists.newArrayList();
        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            resources.add(parseJsonElementToELBResource(region, it.next()));
        }

        Map<String, List<String>> elBtoASGMap = buildELBtoASGMap(region);
        for(Resource resource : resources) {
            List<String> asgList = elBtoASGMap.get(resource.getId());
            if (asgList != null && asgList.size() > 0) {
                resource.setAdditionalField("referencedASGCount", "" + asgList.size());
                String asgStr = StringUtils.join(asgList,",");
                resource.setDescription(resource.getDescription() + ", ASGS=" + asgStr);
                LOGGER.debug(String.format("Resource ELB %s is referenced by ASGs %s", resource.getId(), asgStr));
            } else {
                resource.setAdditionalField("referencedASGCount", "0");
                resource.setDescription(resource.getDescription() + ", ASGS=none");
                LOGGER.debug(String.format("No ASGs found for ELB %s", resource.getId()));
            }
        }

        Map<String, List<DNSEntry>> elBtoDNSMap = buildELBtoDNSMap(region);
        for(Resource resource : resources) {
            List<DNSEntry> dnsEntryList = elBtoDNSMap.get(resource.getAdditionalField("DNSName"));
            if (dnsEntryList != null && dnsEntryList.size() > 0) {
                ArrayList<String> dnsNames = new ArrayList<>();
                ArrayList<String> dnsTypes = new ArrayList<>();
                ArrayList<String> hostedZoneIds = new ArrayList<>();
                for (DNSEntry dnsEntry : dnsEntryList) {
                    dnsNames.add(dnsEntry.dnsName);
                    dnsTypes.add(dnsEntry.dnsType);
                    hostedZoneIds.add(dnsEntry.hostedZoneId);
                }

                resource.setAdditionalField("referencedDNS", StringUtils.join(dnsNames,","));
                resource.setAdditionalField("referencedDNSTypes", StringUtils.join(dnsTypes,","));
                resource.setAdditionalField("referencedDNSZones", StringUtils.join(hostedZoneIds,","));

                resource.setDescription(resource.getDescription() + ", DNS=" + resource.getAdditionalField("referencedDNS"));
                LOGGER.debug(String.format("Resource ELB %s is referenced by DNS %s", resource.getId(), resource.getAdditionalField("referencedDNS")));
            } else {
                resource.setAdditionalField("referencedDNS", "");
                resource.setDescription(resource.getDescription() + ", DNS=none");
                LOGGER.debug(String.format("No DNS found for ELB %s", resource.getId()));
            }
        }

        return resources;
    }

    private Map<String, List<String>> buildELBtoASGMap(String region) {
        String url = eddaClient.getBaseUrl(region) + "/aws/autoScalingGroups;_expand:(autoScalingGroupName,loadBalancerNames)";
        LOGGER.info(String.format("Getting all ELBs associated with ASGs in region %s", region));

        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get JSON node from edda for ASG ELBs in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }

        HashMap<String, List<String>> asgMap = new HashMap<>();
        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            JsonNode asgNode = it.next();
            String asgName = asgNode.get("autoScalingGroupName").getTextValue();
            JsonNode elbs = asgNode.get("loadBalancerNames");
            if (elbs == null || !elbs.isArray() || elbs.size() == 0) {
                continue;
            } else {
                for (Iterator<JsonNode> elbNode = elbs.getElements(); elbNode.hasNext();) {
                    JsonNode elb = elbNode.next();
                    String elbName = elb.getTextValue();

                    List<String> asgList = asgMap.get(elbName);
                    if (asgList == null) {
                        asgList = new ArrayList<>();
                        asgMap.put(elbName, asgList);
                    }
                    asgList.add(asgName);
                    LOGGER.debug(String.format("Found ASG %s associated with ELB %s", asgName, elbName));
                }
            }
        }
        return asgMap;
    }

    private Resource parseJsonElementToELBResource(String region, JsonNode jsonNode) {
        Validate.notNull(jsonNode);

        String elbName = jsonNode.get("loadBalancerName").getTextValue();
        long launchTime = jsonNode.get("createdTime").getLongValue();

        Resource resource = new AWSResource().withId(elbName).withRegion(region)
                .withResourceType(AWSResourceType.ELB)
                .withLaunchTime(new Date(launchTime));

        String dnsName = jsonNode.get("DNSName").getTextValue();
        resource.setAdditionalField("DNSName", dnsName);

        JsonNode tags = jsonNode.get("tags");
        if (tags == null || !tags.isArray() || tags.size() == 0) {
            LOGGER.debug(String.format("No tags is found for %s", resource.getId()));
        } else {
            for (Iterator<JsonNode> it = tags.getElements(); it.hasNext();) {
                JsonNode tag = it.next();
                String key = tag.get("key").getTextValue();
                String value = tag.get("value").getTextValue();
                resource.setTag(key, value);
            }
        }

        String owner = getOwnerEmailForResource(resource);
        if (owner != null) {
            resource.setOwnerEmail(owner);
        }
        LOGGER.debug(String.format("Owner of ELB Resource %s (ELB DNS: %s) is %s", resource.getId(), resource.getAdditionalField("DNSName"), resource.getOwnerEmail()));

        JsonNode instances = jsonNode.get("instances");
        if (instances == null || !instances.isArray() || instances.size() == 0) {
            resource.setAdditionalField("instanceCount", "0");
            resource.setDescription("instances=none");
            LOGGER.debug(String.format("No instances found for ELB %s", resource.getId()));
        } else {
            resource.setAdditionalField("instanceCount", "" + instances.size());
            ArrayList<String> instanceList = new ArrayList<String>(instances.size());
            LOGGER.debug(String.format("Found %d instances for ELB %s", instances.size(), resource.getId()));
            for (Iterator<JsonNode> it = instances.getElements(); it.hasNext();) {
                JsonNode instance = it.next();
                String instanceId = instance.get("instanceId").getTextValue();
                instanceList.add(instanceId);
            }
            String instancesStr = StringUtils.join(instanceList,",");
            resource.setDescription(String.format("instances=%s", instances));
            LOGGER.debug(String.format("Resource ELB %s has instances %s", resource.getId(), instancesStr));
        }

        return resource;
    }

    private Map<String, List<DNSEntry>> buildELBtoDNSMap(String region) {
        String url = eddaClient.getBaseUrl(region) + "/aws/hostedRecords;_expand:(name,type,aliasTarget,resourceRecords:(value),zone:(id))";
        LOGGER.info(String.format("Getting all ELBs associated with DNSs in region %s", region));

        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Failed to get JSON node from edda for DNS ELBs in region %s.", region), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("Failed to get valid document from %s, got: %s", url, jsonNode));
        }

        HashMap<String, List<DNSEntry>> dnsMap = new HashMap<>();
        for (Iterator<JsonNode> it = jsonNode.getElements(); it.hasNext();) {
            JsonNode dnsNode = it.next();
            String dnsName = dnsNode.get("name").getTextValue();
            String dnsType = dnsNode.get("type").getTextValue();
            String hostedZoneId = null;
            JsonNode hostedZoneNode = dnsNode.get("zone");
            if (hostedZoneNode  != null) {
                JsonNode hostedZoneIdNode = hostedZoneNode.get("id");
                if (hostedZoneIdNode != null) {
                    hostedZoneId = hostedZoneIdNode.getTextValue();
                }
            }

            JsonNode aliasTarget = dnsNode.get("aliasTarget");
            if (aliasTarget != null) {
                JsonNode aliasTargetDnsNameNode = aliasTarget.get("DNSName");
                if (aliasTargetDnsNameNode != null) {
                    String aliasTargetDnsName = aliasTargetDnsNameNode.getTextValue();
                    if (aliasTargetDnsName != null && aliasTargetDnsName.contains(".elb.")) {
                        DNSEntry dnsEntry = new DNSEntry();
                        dnsEntry.dnsName = dnsName;
                        dnsEntry.dnsType = dnsType;
                        dnsEntry.hostedZoneId = hostedZoneId;

                        if (aliasTargetDnsName.endsWith(".")) {
                            aliasTargetDnsName = aliasTargetDnsName.substring(0, aliasTargetDnsName.length()-1);
                        }
                        List<DNSEntry> dnsEntryList = dnsMap.get(aliasTargetDnsName);
                        if (dnsEntryList == null) {
                            dnsEntryList = new ArrayList<>();
                            dnsMap.put(aliasTargetDnsName, dnsEntryList);
                        }
                        dnsEntryList.add(dnsEntry);
                        LOGGER.debug(String.format("Found DNS %s (alias) associated with ELB DNS %s, type %s, zone %s", dnsName, aliasTargetDnsName, dnsType, hostedZoneId));
                    }
                }
            }
            JsonNode records = dnsNode.get("resourceRecords");
            if (records == null || !records.isArray() || records.size() == 0) {
                continue;
            } else {
                for (Iterator<JsonNode> recordNode = records.getElements(); recordNode.hasNext();) {
                    JsonNode record = recordNode.next();
                    String elbDNS = record.get("value").getTextValue();
                    if (elbDNS.contains(".elb.")) {
                        DNSEntry dnsEntry = new DNSEntry();
                        dnsEntry.dnsName = dnsName;
                        dnsEntry.dnsType = dnsType;
                        dnsEntry.hostedZoneId = hostedZoneId;

                        List<DNSEntry> dnsEntryList = dnsMap.get(elbDNS);
                        if (dnsEntryList == null) {
                            dnsEntryList = new ArrayList<>();
                            dnsMap.put(elbDNS, dnsEntryList);
                        }
                        dnsEntryList.add(dnsEntry);
                        LOGGER.debug(String.format("Found DNS %s associated with ELB DNS %s, type %s, zone %s", dnsName, elbDNS, dnsType, hostedZoneId));
                    }
                }
            }
        }
        return dnsMap;
    }

}

/*
 *
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
package com.netflix.simianarmy.conformity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The class implementing clusters. Cluster is the basic unit of conformity check. It can be a single ASG or
 * a group of ASGs that belong to the same application, for example, a cluster in the Asgard deployment system.
 */
public class Cluster {
    public static final String OWNER_EMAIL = "ownerEmail";
    public static final String CLUSTER = "cluster";
    public static final String REGION = "region";
    public static final String IS_CONFORMING = "isConforming";
    public static final String IS_OPTEDOUT = "isOptedOut";
    public static final String UPDATE_TIMESTAMP = "updateTimestamp";
    public static final String EXCLUDED_RULES = "excludedRules";
    public static final String CONFORMITY_RULES = "conformityRules";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final String name;
    private final Collection<AutoScalingGroup> autoScalingGroups = Lists.newArrayList();
    private final String region;
    private String ownerEmail;
    private Date updateTime;
    private final Map<String, Conformity> conformities = Maps.newHashMap();
    private final Collection<String> excludedConformityRules = Sets.newHashSet();
    private boolean isConforming;
    private boolean isOptOutOfConformity;
    private final Set<String> soloInstances = Sets.newHashSet();

    /**
     * Constructor.
     * @param name
     *          the name of the cluster
     * @param autoScalingGroups
     *          the auto scaling groups in the cluster
     */
    public Cluster(String name, String region, AutoScalingGroup... autoScalingGroups) {
        Validate.notNull(name);
        Validate.notNull(region);
        Validate.notNull(autoScalingGroups);
        this.name = name;
        this.region = region;
        for (AutoScalingGroup asg : autoScalingGroups) {
            this.autoScalingGroups.add(asg);
        }
    }

    /**
     * Constructor.
     * @param name
     *          the name of the cluster
     * @param soloInstances
     *          the list of all instances
     */
    public Cluster(String name, String region, Set<String> soloInstances) {
        Validate.notNull(name);
        Validate.notNull(region);
        Validate.notNull(soloInstances);
        this.name = name;
        this.region = region;
        for (String soleInstance : soloInstances) {
            this.soloInstances.add(soleInstance);
        }
    }

    /**
     * Gets the name of the cluster.
     * @return
     *      the name of the cluster
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the region of the cluster.
     * @return
     *      the region of the cluster
     */
    public String getRegion() {
        return region;
    }

    /**
     * * Gets the auto scaling groups of the auto scaling group.
     * @return
     *    the auto scaling groups in the cluster
     */
    public Collection<AutoScalingGroup> getAutoScalingGroups() {
        return Collections.unmodifiableCollection(autoScalingGroups);
    }

    /**
     * Gets the owner email of the cluster.
     * @return
     *      the owner email of the cluster
     */
    public String getOwnerEmail() {
        return ownerEmail;
    }

    /**
     * Sets the owner email of the cluster.
     * @param ownerEmail
     *              the owner email of the cluster
     */
    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    /**
     * Gets the update time of the cluster.
     * @return
     *      the update time of the cluster
     */
    public Date getUpdateTime() {
        return new Date(updateTime.getTime());
    }

    /**
     * Sets the update time of the cluster.
     * @param updateTime
     *              the update time of the cluster
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = new Date(updateTime.getTime());
    }

    /**
     * Gets all conformity check information of the cluster.
     * @return
     *      all conformity check information of the cluster
     */
    public Collection<Conformity> getConformties() {
        return conformities.values();
    }

    /**
     * Gets the conformity information for a conformity rule.
     * @param rule
     *          the conformity rule
     * @return
     *          the conformity for the rule
     */
    public Conformity getConformity(ConformityRule rule) {
        Validate.notNull(rule);
        return conformities.get(rule.getName());
    }

    /**
     * Updates the cluster with a new conformity check result.
     * @param conformity
     *          the conformity to update
     * @return
     *          the cluster itself
     *
     */
    public Cluster updateConformity(Conformity conformity) {
        Validate.notNull(conformity);
        conformities.put(conformity.getRuleId(), conformity);
        return this;
    }

    /**
     * Clears the conformity check results.
     */
    public void clearConformities() {
        conformities.clear();
    }

    /**
     * Gets the boolean flag to indicate whether the cluster is conforming to
     * all non-excluded conformity rules.
     * @return
     *      true if the cluster is conforming against all non-excluded rules,
     *      false otherwise
     */
    public boolean isConforming() {
        return isConforming;
    }

    /**
     * Sets the boolean flag to indicate whether the cluster is conforming to
     * all non-excluded conformity rules.
     * @param conforming
     *      true if the cluster is conforming against all non-excluded rules,
     *      false otherwise
     */
    public void setConforming(boolean conforming) {
        isConforming = conforming;
    }

    /**
     * Gets names of all excluded conformity rules for this cluster.
     * @return
     *      names of all excluded conformity rules for this cluster
     */
    public Collection<String> getExcludedRules() {
        return Collections.unmodifiableCollection(excludedConformityRules);
    }

    /**
     * Excludes rules for the cluster.
     * @param ruleIds
     *          the rule ids to exclude
     * @return
     *          the cluster itself
     */
    public Cluster excludeRules(String... ruleIds) {
        Validate.notNull(ruleIds);
        for (String ruleId : ruleIds) {
            Validate.notNull(ruleId);
            excludedConformityRules.add(ruleId.trim());
        }
        return this;
    }

    /**
     * Gets the flag to indicate whether the cluster is opted out of Conformity monkey.
     * @return true if the cluster is not handled by Conformity monkey, false otherwise
     */
    public boolean isOptOutOfConformity() {
        return isOptOutOfConformity;
    }

    /**
     * Sets the flag to indicate whether the cluster is opted out of Conformity monkey.
     * @param optOutOfConformity
     *            true if the cluster is not handled by Conformity monkey, false otherwise
     */
    public void setOptOutOfConformity(boolean optOutOfConformity) {
        isOptOutOfConformity = optOutOfConformity;
    }

    /**
     * Gets a map from fields of resources to corresponding values. Values are represented
     * as Strings so they can be displayed or stored in databases like SimpleDB.
     * @return a map from field name to field value
     */
    public Map<String, String> getFieldToValueMap() {
        Map<String, String> map = Maps.newHashMap();
        putToMapIfNotNull(map, CLUSTER, name);
        putToMapIfNotNull(map, REGION, region);
        putToMapIfNotNull(map, OWNER_EMAIL, ownerEmail);
        putToMapIfNotNull(map, UPDATE_TIMESTAMP, String.valueOf(DATE_FORMATTER.print(updateTime.getTime())));
        putToMapIfNotNull(map, IS_CONFORMING, String.valueOf(isConforming));
        putToMapIfNotNull(map, IS_OPTEDOUT, String.valueOf(isOptOutOfConformity));
        putToMapIfNotNull(map, EXCLUDED_RULES, StringUtils.join(excludedConformityRules, ","));
        List<String> ruleIds = Lists.newArrayList();
        for (Conformity conformity : conformities.values()) {
            map.put(conformity.getRuleId(), StringUtils.join(conformity.getFailedComponents(), ","));
            ruleIds.add(conformity.getRuleId());
        }
        putToMapIfNotNull(map, CONFORMITY_RULES, StringUtils.join(ruleIds, ","));
        return map;
    }

    /**
     * Parse a map from field name to value to a cluster.
     * @param fieldToValue the map from field name to value
     * @return the cluster that is de-serialized from the map
     */
    public static Cluster parseFieldToValueMap(Map<String, String> fieldToValue) {
        Validate.notNull(fieldToValue);
        Cluster cluster = new Cluster(fieldToValue.get(CLUSTER),
                fieldToValue.get(REGION));
        cluster.setOwnerEmail(fieldToValue.get(OWNER_EMAIL));
        cluster.setConforming(Boolean.parseBoolean(fieldToValue.get(IS_CONFORMING)));
        cluster.setOptOutOfConformity(Boolean.parseBoolean(fieldToValue.get(IS_OPTEDOUT)));
        cluster.excludeRules(StringUtils.split(fieldToValue.get(EXCLUDED_RULES), ","));
        cluster.setUpdateTime(new Date(DATE_FORMATTER.parseDateTime(fieldToValue.get(UPDATE_TIMESTAMP)).getMillis()));
        for (String ruleId : StringUtils.split(fieldToValue.get(CONFORMITY_RULES), ",")) {
            cluster.updateConformity(new Conformity(ruleId,
                    Lists.newArrayList(StringUtils.split(fieldToValue.get(ruleId), ","))));
        }
        return cluster;
    }

    private static void putToMapIfNotNull(Map<String, String> map, String key, String value) {
        Validate.notNull(map);
        Validate.notNull(key);
        if (value != null) {
            map.put(key, value);
        }
    }

    public Set<String> getSoloInstances() {
        return Collections.unmodifiableSet(soloInstances);
    }

}

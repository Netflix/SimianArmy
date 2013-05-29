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
package com.netflix.simianarmy.basic.conformity;

import com.google.common.collect.Maps;
import com.netflix.simianarmy.conformity.Cluster;
import com.netflix.simianarmy.conformity.Conformity;
import com.netflix.simianarmy.conformity.ConformityEmailBuilder;
import com.netflix.simianarmy.conformity.ConformityRule;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/** The basic implementation of the email builder for Conformity monkey. */
public class BasicConformityEmailBuilder extends ConformityEmailBuilder {
    private static final String[] TABLE_COLUMNS = {"Cluster", "Region", "Rule", "Failed Components"};
    private static final String AHREF_TEMPLATE = "<a href=\"%s\">%s</a>";
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicConformityEmailBuilder.class);

    private Map<String, Collection<Cluster>> emailToClusters;
    private final Map<String, ConformityRule> idToRule = Maps.newHashMap();

    @Override
    public void setEmailToClusters(Map<String, Collection<Cluster>> clustersByEmail, Collection<ConformityRule> rules) {
        Validate.notNull(clustersByEmail);
        Validate.notNull(rules);
        this.emailToClusters = clustersByEmail;
        idToRule.clear();
        for (ConformityRule rule : rules) {
            idToRule.put(rule.getName(), rule);
        }
    }

    @Override
    protected String getHeader() {
        StringBuilder header = new StringBuilder();
        header.append("<b><h2>Conformity Report</h2></b>");
        header.append("The following is a list of failed conformity rules for your cluster(s).<br/>");
        return header.toString();
    }

    @Override
    protected String getEntryTable(String emailAddress) {
        StringBuilder table = new StringBuilder();
        table.append(getHtmlTableHeader(getTableColumns()));
        for (Cluster cluster : emailToClusters.get(emailAddress)) {
            for (Conformity conformity : cluster.getConformties()) {
                if (!conformity.getFailedComponents().isEmpty()) {
                    table.append(getClusterRow(cluster, conformity));
                }
            }
        }
        table.append("</table>");
        return table.toString();
    }

    @Override
    protected String getFooter() {
        return "<br/>Conformity Monkey wiki: https://github.com/Netflix/SimianArmy/wiki<br/>";
    }

    /**
     * Gets the url to view the details of the cluster.
     * @param cluster the cluster
     * @return the url to view/edit the cluster.
     */
    protected String getClusterUrl(Cluster cluster) {
        return null;
    }

    /**
     * Gets the string when displaying the cluster, e.g. the id.
     * @param cluster the cluster to display
     * @return the string to represent the cluster
     */
    protected String getClusterDisplay(Cluster cluster) {
        return cluster.getName();
    }

    /** Gets the table columns for the table in the email.
     *
     * @return the array of column names
     */
    protected String[] getTableColumns() {
        return TABLE_COLUMNS;
    }

    /**
     * Gets the row for a cluster and a failed conformity check in the table in the email body.
     * @param cluster the cluster to display
     * @param conformity the failed conformity check
     * @return the table row in the email body
     */
    protected String getClusterRow(Cluster cluster, Conformity conformity) {
        StringBuilder message = new StringBuilder();
        message.append("<tr>");
        String clusterUrl = getClusterUrl(cluster);
        if (!StringUtils.isEmpty(clusterUrl)) {
            message.append(getHtmlCell(String.format(AHREF_TEMPLATE, clusterUrl, getClusterDisplay(cluster))));
        } else {
            message.append(getHtmlCell(getClusterDisplay(cluster)));
        }
        message.append(getHtmlCell(cluster.getRegion()));
        ConformityRule rule = idToRule.get(conformity.getRuleId());
        String ruleDesc;
        if (rule == null) {
            LOGGER.warn(String.format("Not found rule with name %s", conformity.getRuleId()));
            ruleDesc = conformity.getRuleId();
        } else {
            ruleDesc = rule.getNonconformingReason();
        }
        message.append(getHtmlCell(ruleDesc));
        message.append(getHtmlCell(StringUtils.join(conformity.getFailedComponents(), ",")));
        message.append("</tr>");
        return message.toString();
    }

}

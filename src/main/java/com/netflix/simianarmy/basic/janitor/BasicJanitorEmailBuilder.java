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
package com.netflix.simianarmy.basic.janitor;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.janitor.JanitorEmailBuilder;

/** The basic implementation of the email builder for Janitor monkey. */
public class BasicJanitorEmailBuilder extends JanitorEmailBuilder {
    private static final String[] TABLE_COLUMNS =
        {"Resource Type", "Resource", "Region", "Description", "Expected Termination Time",
        "Termination Reason", "View/Edit"};
    private static final String AHREF_TEMPLATE = "<a href=\"%s\">%s</a>";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("EEE, MMM dd, yyyy");

    private Map<String, Collection<Resource>> emailToResources;

    @Override
    public void setEmailToResources(Map<String, Collection<Resource>> emailToResources) {
        Validate.notNull(emailToResources);
        this.emailToResources = emailToResources;
    }

    @Override
    protected String getHeader() {
        StringBuilder header = new StringBuilder();
        header.append("<b><h2>Janitor Notifications</h2></b>");
        header.append(
                "The following resource(s) have been marked for cleanup by Janitor monkey "
                        + "as potential unused resources. This is a non-repeating notification.<br/>");
        return header.toString();
    }

    @Override
    protected String getEntryTable(String emailAddress) {
        StringBuilder table = new StringBuilder();
        table.append(getHtmlTableHeader(getTableColumns()));
        for (Resource resource : emailToResources.get(emailAddress)) {
            table.append(getResourceRow(resource));
        }
        table.append("</table>");
        return table.toString();
    }

    @Override
    protected String getFooter() {
        return "<br/>Janitor Monkey wiki: https://github.com/Netflix/SimianArmy/wiki<br/>";
    }

    /**
     * Gets the url to view the details of the resource.
     * @param resource the resource
     * @return the url to view/edit the resource.
     */
    protected String getResourceUrl(Resource resource) {
        return null;
    }

    /**
     * Gets the string when displaying the resource, e.g. the id.
     * @param resource the resource to display
     * @return the string to represent the resource
     */
    protected String getResourceDisplay(Resource resource) {
        return resource.getId();
    }

    /**
     * Gets the url to edit the Janitor termination of the resource.
     * @param resource the resource
     * @return the url to edit the Janitor termination the resource.
     */
    protected String getJanitorResourceUrl(Resource resource) {
        return null;
    }

    /** Gets the table columns for the table in the email.
     *
     * @return the array of column names
     */
    protected String[] getTableColumns() {
        return TABLE_COLUMNS;
    }

    /**
     * Gets the row for a resource in the table in the email body.
     * @param resource the resource to display
     * @return the table row in the email body
     */
    protected String getResourceRow(Resource resource) {
        StringBuilder message = new StringBuilder();
        message.append("<tr>");
        message.append(getHtmlCell(resource.getResourceType().name()));
        String resourceUrl = getResourceUrl(resource);
        if (!StringUtils.isEmpty(resourceUrl)) {
            message.append(getHtmlCell(String.format(AHREF_TEMPLATE, resourceUrl, getResourceDisplay(resource))));
        } else {
            message.append(getHtmlCell(getResourceDisplay(resource)));
        }
        message.append(getHtmlCell(resource.getRegion()));
        if (resource.getDescription() == null) {
            message.append(getHtmlCell(""));
        } else {
            message.append(getHtmlCell(resource.getDescription().replace(";", "<br/>").replace(",", "<br/>")));
        }
        message.append(getHtmlCell(DATE_FORMATTER.print(resource.getExpectedTerminationTime().getTime())));
        message.append(getHtmlCell(resource.getTerminationReason()));
        String janitorUrl = getJanitorResourceUrl(resource);
        if (!StringUtils.isEmpty(janitorUrl)) {
            message.append(getHtmlCell(String.format(AHREF_TEMPLATE, janitorUrl, "View/Extend")));
        } else {
            message.append(getHtmlCell(""));
        }
        message.append("</tr>");
        return message.toString();
    }

}

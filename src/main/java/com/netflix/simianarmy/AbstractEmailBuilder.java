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
package com.netflix.simianarmy;

/** The abstract email builder. */
public abstract class AbstractEmailBuilder implements EmailBuilder {

    @Override
    public String buildEmailBody(String emailAddress) {
        StringBuilder body = new StringBuilder();
        String header = getHeader();
        if (header != null) {
            body.append(header);
        }
        String entryTable = getEntryTable(emailAddress);
        if (entryTable != null) {
            body.append(entryTable);
        }
        String footer = getFooter();
        if (footer != null) {
            body.append(footer);
        }
        return body.toString();
    }

    /**
     * Gets the header to the email body.
     */
    protected abstract String getHeader();

    /**
     * Gets the table of entries in the email body.
     * @param emailAddress the email address to notify
     * @return the HTML string representing the table for the resources to send to the
     * email address
     */
    protected abstract String getEntryTable(String emailAddress);

    /**
     * Gets the footer of the email body.
     */
    protected abstract String getFooter();

    /**
     * Gets the HTML cell in the table of a string value.
     * @param value the string to put in the table
     * @return the HTML text
     */
    protected String getHtmlCell(String value) {
        return "<td style=\"padding: 4px\">" + value + "</td>";
    }

    /**
     * Gets the HTML string displaying the table header with the specified column names.
     * @param columns the column names for the table
     */
    protected String getHtmlTableHeader(String[] columns) {
        StringBuilder tableHeader = new StringBuilder();
        tableHeader.append(
                "<table border=\"1\" style=\"border-width:1px; border-spacing: 0px; border-collapse: seperate;\">");
        tableHeader.append("<tr style=\"background-color: #E8E8E8;\" >");
        for (String col : columns) {
            tableHeader.append(getHtmlCell(col));
        }
        tableHeader.append("</tr>");
        return tableHeader.toString();
    }
}

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

import com.netflix.simianarmy.client.edda.EddaClient;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Misc common Edda Utilities
 */
public class EddaUtils {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EddaUtils.class);

    public static Map<String, String> getAllApplicationOwnerEmails(EddaClient eddaClient) {
        String region = "us-east-1";
        LOGGER.info(String.format("Getting all application names and emails in region %s.", region));

        String url = eddaClient.getBaseUrl(region) + "/netflix/applications/;_expand:(name,email)";
        JsonNode jsonNode = null;
        try {
            jsonNode = eddaClient.getJsonNodeFromUrl(url);
        } catch (UnknownHostException e) {
            LOGGER.warn(String.format("Edda endpoint is not available in region %s", region));
            return Collections.emptyMap();
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to get Json node from url: %s", url), e);
        }

        if (jsonNode == null || !jsonNode.isArray()) {
            throw new RuntimeException(String.format("failed to get valid document from %s, got: %s", url, jsonNode));
        }

        Iterator<JsonNode> it = jsonNode.getElements();
        Map<String, String> appToOwner = new HashMap<String, String>();
        while (it.hasNext()) {
            JsonNode node = it.next();
            String appName = node.get("name").getTextValue().toLowerCase();
            String owner = node.get("email").getTextValue();
            if (appName != null && owner != null) {
                appToOwner.put(appName, owner);
            }
        }
        return appToOwner;
    }

}

package com.netflix.simianarmy.client;

import org.apache.commons.lang.Validate;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * A REST client used by monkeys.
 */
public abstract class MonkeyRestClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(MonkeyRestClient.class);

    private final HttpClient httpClient;

    /**
     * Constructor.
     * @param timeout the timeout in milliseconds
     * @param maxRetries the max number of retries
     * @param retryInterval the interval in milliseconds between retries
     */
    public MonkeyRestClient(int timeout, int maxRetries, int retryInterval) {
        Validate.isTrue(timeout >= 0);
        Validate.isTrue(maxRetries >= 0);
        Validate.isTrue(retryInterval > 0);
        
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .build();
        httpClient = HttpClientBuilder.create()
            .setDefaultRequestConfig(config)
            .setServiceUnavailableRetryStrategy(new DefaultServiceUnavailableRetryStrategy(maxRetries, retryInterval))
            .build();
    }

    /**
     * Gets the response in JSON from a url.
     * @param url the url
     * @return the JSON node for the response
     */
    // CHECKSTYLE IGNORE MagicNumberCheck
    public JsonNode getJsonNodeFromUrl(String url) throws IOException {
        LOGGER.info(String.format("Getting Json response from url: %s", url));
        HttpGet request = new HttpGet(url);
        request.setHeader("Accept", "application/json");
        HttpResponse response = httpClient.execute(request);

        InputStream is = response.getEntity().getContent();
        String jsonContent;
        if (is != null) {
            Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
            jsonContent = s.hasNext() ? s.next() : "";
            is.close();
        } else {
            return null;
        }

        int code = response.getStatusLine().getStatusCode();
        if (code == 404) {
            return null;
        } else if (code >= 300 || code < 200) {
            throw new RuntimeException(String.format("Response code %d from url %s: %s", code, url, jsonContent));
        }

        JsonNode result;
        try {
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.readTree(jsonContent);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error trying to parse json response from url %s, got: %s",
                    url, jsonContent), e);
        }
        return result;
    }

    /**
     * Gets the base url of the service for a specific region.
     * @param region the region
     * @return the base url in the region
     */
    public abstract String getBaseUrl(String region);
}

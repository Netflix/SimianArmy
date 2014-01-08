package com.netflix.simianarmy.client.edda;

import com.netflix.simianarmy.MonkeyConfiguration;
import com.netflix.simianarmy.client.MonkeyRestClient;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The REST client to access Edda to get the history of a cloud resource.
 */
public class EddaClient extends MonkeyRestClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(EddaClient.class);

    private final MonkeyConfiguration config;
    /**
     * Constructor.
     * @param timeout the timeout in milliseconds
     * @param maxRetries the max number of retries
     * @param retryInterval the interval in milliseconds between retries
     * @param config the monkey configuration
     */
    public EddaClient(int timeout, int maxRetries, int retryInterval, MonkeyConfiguration config) {
        super(timeout, maxRetries, retryInterval);
        this.config = config;
    }

    @Override
    public String getBaseUrl(String region) {
        Validate.notEmpty(region);
        String baseUrl = config.getStr("simianarmy.janitor.edda.endpoint." + region);
        if (StringUtils.isBlank(baseUrl)) {
            LOGGER.error(String.format("No endpoint of Edda is found for region %s.", region));
        }
        return baseUrl;
    }
}

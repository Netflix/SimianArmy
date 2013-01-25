package com.netflix.simianarmy.aws.janitor.rule.asg;

import com.netflix.simianarmy.Resource;
import com.netflix.simianarmy.aws.janitor.crawler.ASGJanitorCrawler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dummy implementation of ASGInstanceValidator that considers every instance as active.
 */
public class DummyASGInstanceValidator implements ASGInstanceValidator {
    /** The Constant LOGGER. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyASGInstanceValidator.class);

    /** {@inheritDoc} */
    @Override
    public boolean hasActiveInstance(Resource resource) {
        String instanceIds = resource.getAdditionalField(ASGJanitorCrawler.ASG_FIELD_INSTANCES);
        String maxSizeStr = resource.getAdditionalField(ASGJanitorCrawler.ASG_FIELD_MAX_SIZE);
        if (StringUtils.isBlank(instanceIds)) {
            if (maxSizeStr != null && Integer.parseInt(maxSizeStr) == 0) {
                // The ASG is empty when it has no instance and the max size of the ASG is 0.
                // If the max size is not 0, the ASG could probably be in the process of starting new instances.
                LOGGER.info(String.format("ASG %s is empty.", resource.getId()));
                return false;
            } else {
                LOGGER.info(String.format("ASG %s does not have instances but the max size is %s",
                        resource.getId(), maxSizeStr));
                return true;
            }
        }
        String[] instances = StringUtils.split(instanceIds, ",");
        return instances.length > 0;
    }
}

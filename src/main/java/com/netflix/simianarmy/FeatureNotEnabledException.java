package com.netflix.simianarmy;

/**
 * The Class FeatureNotEnabledException.
 *
 * These exceptions will be thrown when a feature is not enabled when being accessed.
 */
public class FeatureNotEnabledException extends Exception {

    private static final long serialVersionUID = 8392434473284901306L;

    /**
     * Instantiates a FeatureNotEnabledException with a message.
     * @param msg the error message
     */
    public FeatureNotEnabledException(String msg) {
        super(msg);
    }
}

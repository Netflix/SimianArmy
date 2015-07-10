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
package com.netflix.simianarmy.aws;

import java.util.Date;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSSessionCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;

/**
 * AWSCredentialsProvider implementation that uses the AWS Security Token
 * Service to assume a Role and create temporary, short-lived sessions to use
 * for authentication.
 */
public class STSAssumeRoleSessionCredentialsProvider implements AWSCredentialsProvider {

    /** Default duration for started sessions. */
    public static final int DEFAULT_DURATION_SECONDS = 900;

    /** Time before expiry within which credentials will be renewed. */
    private static final int EXPIRY_TIME_MILLIS = 60 * 1000;

    /** The client for starting STS sessions. */
    private final AWSSecurityTokenService securityTokenService;

    /** The current session credentials. */
    private AWSSessionCredentials sessionCredentials;

    /** The expiration time for the current session credentials. */
    private Date sessionCredentialsExpiration;

    /** The arn of the role to be assumed. */
    private String roleArn;

    /**
     * Constructs a new STSAssumeRoleSessionCredentialsProvider, which makes a
     * request to the AWS Security Token Service (STS), uses the provided
     * {@link #roleArn} to assume a role and then request short lived session
     * credentials, which will then be returned by this class's
     * {@link #getCredentials()} method.
     * @param roleArn
     *            The AWS ARN of the Role to be assumed.
     */
    public STSAssumeRoleSessionCredentialsProvider(String roleArn) {
        this.roleArn = roleArn;
        securityTokenService = new AWSSecurityTokenServiceClient();
    }

    /**
     * Constructs a new STSAssumeRoleSessionCredentialsProvider, which makes a
     * request to the AWS Security Token Service (STS), uses the provided
     * {@link #roleArn} to assume a role and then request short lived session
     * credentials, which will then be returned by this class's
     * {@link #getCredentials()} method.
     * @param roleArn
     *            The AWS ARN of the Role to be assumed.
     * @param clientConfiguration
     *            The AWS ClientConfiguration to use when making AWS API requests.
     */
    public STSAssumeRoleSessionCredentialsProvider(String roleArn, ClientConfiguration clientConfiguration) {
        this.roleArn = roleArn;
        securityTokenService = new AWSSecurityTokenServiceClient(clientConfiguration);
    }

    /**
     * Constructs a new STSAssumeRoleSessionCredentialsProvider, which will use
     * the specified long lived AWS credentials to make a request to the AWS
     * Security Token Service (STS), uses the provided {@link #roleArn} to
     * assume a role and then request short lived session credentials, which
     * will then be returned by this class's {@link #getCredentials()} method.
     * @param longLivedCredentials
     *            The main AWS credentials for a user's account.
     * @param roleArn
     *            The AWS ARN of the Role to be assumed.
     */
    public STSAssumeRoleSessionCredentialsProvider(AWSCredentials longLivedCredentials, String roleArn) {
        this(longLivedCredentials, roleArn, new ClientConfiguration());
    }

    /**
     * Constructs a new STSAssumeRoleSessionCredentialsProvider, which will use
     * the specified long lived AWS credentials to make a request to the AWS
     * Security Token Service (STS), uses the provided {@link #roleArn} to
     * assume a role and then request short lived session credentials, which
     * will then be returned by this class's {@link #getCredentials()} method.
     * @param longLivedCredentials
     *            The main AWS credentials for a user's account.
     * @param roleArn
     *            The AWS ARN of the Role to be assumed.
     * @param clientConfiguration
     *            Client configuration connection parameters.
     */
    public STSAssumeRoleSessionCredentialsProvider(AWSCredentials longLivedCredentials, String roleArn,
            ClientConfiguration clientConfiguration) {
        this.roleArn = roleArn;
        securityTokenService = new AWSSecurityTokenServiceClient(longLivedCredentials, clientConfiguration);
    }

    /**
     * Constructs a new STSAssumeRoleSessionCredentialsProvider, which will use
     * the specified credentials provider (which vends long lived AWS
     * credentials) to make a request to the AWS Security Token Service (STS),
     * usess the provided {@link #roleArn} to assume a role and then request
     * short lived session credentials, which will then be returned by this
     * class's {@link #getCredentials()} method.
     * @param longLivedCredentialsProvider
     *            Credentials provider for the main AWS credentials for a user's
     *            account.
     * @param roleArn
     *            The AWS ARN of the Role to be assumed.
     */
    public STSAssumeRoleSessionCredentialsProvider(AWSCredentialsProvider longLivedCredentialsProvider,
            String roleArn) {
        this.roleArn = roleArn;
        securityTokenService = new AWSSecurityTokenServiceClient(longLivedCredentialsProvider);
    }

    /**
     * Constructs a new STSAssumeRoleSessionCredentialsProvider, which will use
     * the specified credentials provider (which vends long lived AWS
     * credentials) to make a request to the AWS Security Token Service (STS),
     * uses the provided {@link #roleArn} to assume a role and then request
     * short lived session credentials, which will then be returned by this
     * class's {@link #getCredentials()} method.
     * @param longLivedCredentialsProvider
     *            Credentials provider for the main AWS credentials for a user's
     *            account.
     * @param roleArn
     *            The AWS ARN of the Role to be assumed.
     * @param clientConfiguration
     *            Client configuration connection parameters.
     */
    public STSAssumeRoleSessionCredentialsProvider(AWSCredentialsProvider longLivedCredentialsProvider, String roleArn,
            ClientConfiguration clientConfiguration) {
        this.roleArn = roleArn;
        securityTokenService = new AWSSecurityTokenServiceClient(longLivedCredentialsProvider, clientConfiguration);
    }

    @Override
    public AWSCredentials getCredentials() {
        if (needsNewSession()) {
            startSession();
        }
        return sessionCredentials;
    }

    @Override
    public void refresh() {
        startSession();
    }

    /**
     * Starts a new session by sending a request to the AWS Security Token
     * Service (STS) to assume a Role using the long lived AWS credentials. This
     * class then vends the short lived session credentials for the assumed Role
     * sent back from STS.
     */
    private void startSession() {
        AssumeRoleResult assumeRoleResult = securityTokenService.assumeRole(new AssumeRoleRequest()
                .withRoleArn(roleArn).withDurationSeconds(DEFAULT_DURATION_SECONDS).withRoleSessionName("SimianArmy"));
        Credentials stsCredentials = assumeRoleResult.getCredentials();
        sessionCredentials = new BasicSessionCredentials(stsCredentials.getAccessKeyId(),
                stsCredentials.getSecretAccessKey(), stsCredentials.getSessionToken());
        sessionCredentialsExpiration = stsCredentials.getExpiration();
    }

    /**
     * Returns true if a new STS session needs to be started. A new STS session
     * is needed when no session has been started yet, or if the last session is
     * within {@link #EXPIRY_TIME_MILLIS} seconds of expiring.
     * @return True if a new STS session needs to be started.
     */
    private boolean needsNewSession() {
        if (sessionCredentials == null) {
            return true;
        }
        long timeRemaining = sessionCredentialsExpiration.getTime() - System.currentTimeMillis();
        return timeRemaining < EXPIRY_TIME_MILLIS;
    }

}

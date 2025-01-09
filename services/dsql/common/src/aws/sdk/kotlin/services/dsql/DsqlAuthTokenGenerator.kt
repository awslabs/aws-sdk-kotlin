/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.dsql

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awssigning.AuthTokenGenerator
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Generates an IAM authentication token for use with DSQL databases
 * @param credentialsProvider The [CredentialsProvider] which will provide credentials to use when generating the auth token, defaults to [DefaultChainCredentialsProvider]
 * @param signer The [AwsSigner] implementation to use when creating the authentication token, defaults to [DefaultAwsSigner]
 * @param clock The [Clock] implementation to use
 */
public class DsqlAuthTokenGenerator(
    public val credentialsProvider: CredentialsProvider = DefaultChainCredentialsProvider(),
    public val signer: AwsSigner = DefaultAwsSigner,
    public val clock: Clock = Clock.System,
) {
    private val generator = AuthTokenGenerator("dsql", credentialsProvider, signer, clock)

    /**
     * Generates an auth token for the DbConnect action.
     * @param endpoint the endpoint of the database
     * @param region the region of the database
     * @param expiration how long the auth token should be valid for. Defaults to 900.seconds
     */
    public suspend fun generateDbConnectAuthToken(endpoint: Url, region: String, expiration: Duration = 900.seconds): String {
        val dbConnectEndpoint = endpoint.toBuilder().apply {
            parameters.decodedParameters.put("Action", "DbConnect")
        }.build()

        return generator.generateAuthToken(dbConnectEndpoint, region, expiration)
    }

    /**
     * Generates an auth token for the DbConnectAdmin action.
     * @param endpoint the endpoint of the database
     * @param region the region of the database
     * @param expiration how long the auth token should be valid for. Defaults to 900.seconds
     */
    public suspend fun generateDbConnectAdminAuthToken(endpoint: Url, region: String, expiration: Duration = 900.seconds): String {
        val dbConnectAdminEndpoint = endpoint.toBuilder().apply {
            parameters.decodedParameters.put("Action", "DbConnectAdmin")
        }.build()

        return generator.generateAuthToken(dbConnectAdminEndpoint, region, expiration)
    }
}

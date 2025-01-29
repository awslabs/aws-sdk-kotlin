/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.rds

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awssigning.AuthTokenGenerator
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.time.Clock
import kotlin.apply
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Generates an IAM authentication token for use with RDS databases
 * @param credentialsProvider The [CredentialsProvider] which will provide credentials to use when generating the auth token, defaults to [DefaultChainCredentialsProvider]
 * @param signer The [AwsSigner] implementation to use when creating the authentication token, defaults to [DefaultAwsSigner]
 * @param clock The [Clock] implementation to use
 */
public class RdsAuthTokenGenerator(
    public val credentialsProvider: CredentialsProvider = DefaultChainCredentialsProvider(),
    public val signer: AwsSigner = DefaultAwsSigner,
    public val clock: Clock = Clock.System,
) {
    private val generator = AuthTokenGenerator("rds-db", credentialsProvider, signer, clock)

    /**
     * Generates an auth token for the `connect` action.
     * @param endpoint the endpoint of the database
     * @param region the region of the database
     * @param username the username to authenticate with
     * @param expiration how long the auth token should be valid for. Defaults to 900.seconds
     */
    public suspend fun generateAuthToken(endpoint: Url, region: String, username: String, expiration: Duration = 900.seconds): String {
        val endpoint = endpoint.toBuilder().apply {
            parameters.decodedParameters.apply {
                put("Action", "connect")
                put("DBUser", username)
            }
        }.build()

        return generator.generateAuthToken(endpoint, region, expiration)
    }
}

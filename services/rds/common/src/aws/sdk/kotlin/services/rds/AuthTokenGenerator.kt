/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.rds

import aws.sdk.kotlin.runtime.auth.AuthTokenGenerator
import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.runBlocking
import kotlin.apply
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Generates an IAM authentication token for use with RDS databases
 * @param credentials The credentials to use when generating the auth token, defaults to resolving credentials from the [DefaultChainCredentialsProvider]
 */
public class AuthTokenGenerator(
    public val credentials: Credentials? = runBlocking { DefaultChainCredentialsProvider().resolve() },
) {
    private val generator = AuthTokenGenerator("rds-db", credentials)

    /**
     * Generates an auth token for the DbConnect action.
     * @param endpoint the endpoint of the database
     * @param region the region of the database
     * @param expiration how long the auth token should be valid for. Defaults to 900.seconds
     */
    public suspend fun generateAuthToken(endpoint: Url, region: String, username: String, expiration: Duration = 900.seconds): String {
        val dbConnectEndpoint = endpoint.toBuilder().apply {
            parameters.apply {
                decodedParameters {
                    add("Action", "connect")
                    add("DBUser", username)
                }
            }
        }.build()

        return generator.generateAuthToken(dbConnectEndpoint, region, expiration)
    }
}

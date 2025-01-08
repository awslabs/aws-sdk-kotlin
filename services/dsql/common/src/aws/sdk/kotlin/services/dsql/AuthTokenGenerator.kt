/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.dsql

import aws.sdk.kotlin.runtime.auth.AuthTokenGenerator
import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.net.url.Url
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Generates an IAM authentication token for use with DSQL databases
 * @param credentials The credentials to use when generating the auth token, defaults to resolving credentials from the [DefaultChainCredentialsProvider]
 */
public class AuthTokenGenerator(
    public val credentials: Credentials? = runBlocking { DefaultChainCredentialsProvider().resolve() },
) {
    private val generator = AuthTokenGenerator("dsql", credentials)

    /**
     * Generates an auth token for the DbConnect action.
     * @param endpoint the endpoint of the database
     * @param region the region of the database
     * @param expiration how long the auth token should be valid for. Defaults to 900.seconds
     */
    public suspend fun generateDbConnectAuthToken(endpoint: Url, region: String, expiration: Duration = 900.seconds): String {
        val dbConnectEndpoint = endpoint.toBuilder().apply {
            parameters.apply {
                decodedParameters {
                    add("Action", "DbConnect")
                }
            }
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
            parameters.apply {
                decodedParameters {
                    add("Action", "DbConnectAdmin")
                }
            }
        }.build()

        return generator.generateAuthToken(dbConnectAdminEndpoint, region, expiration)
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.dsql

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSignatureType
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.time.Clock
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Generates an IAM authentication token for use with DSQL databases
 * @param credentials The credentials to use when generating the auth token, defaults to resolving credentials from the [DefaultChainCredentialsProvider]
 */
public class AuthTokenGenerator(
    public val credentials: Credentials? = runBlocking { DefaultChainCredentialsProvider().resolve() }
) {
    private fun String.trimScheme() = removePrefix("http://").removePrefix("https://")

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

        return generateAuthToken(dbConnectEndpoint, region, expiration)
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

        return generateAuthToken(dbConnectAdminEndpoint, region, expiration)
    }

    private suspend fun generateAuthToken(endpoint: Url, region: String, expiration: Duration): String {
        val req = HttpRequest(HttpMethod.GET, endpoint)

        val creds = credentials

        val config = AwsSigningConfig {
            credentials = creds ?: DefaultChainCredentialsProvider().resolve()
            this.region = region
            service = "dsql"
            signingDate = Clock.System.now()
            expiresAfter = expiration
            signatureType = AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS
        }

        return DefaultAwsSigner.sign(req, config).output.url.toString().trimScheme()
    }
}
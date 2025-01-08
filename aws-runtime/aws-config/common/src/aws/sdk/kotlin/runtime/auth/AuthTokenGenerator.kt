/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSignatureType
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig.Companion.invoke
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.time.Clock
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration

/**
 * Generates an authentication token, which is a SigV4-signed URL with the HTTP scheme removed.
 * @param service The name of the service the token is being generated for
 * @param credentials The credentials to use when generating the auth token, defaults to resolving credentials from the [DefaultChainCredentialsProvider]
 */
public class AuthTokenGenerator(
    public val service: String,
    public val credentials: Credentials? = runBlocking { DefaultChainCredentialsProvider().resolve() },
) {
    private fun String.trimScheme() = removePrefix("http://").removePrefix("https://")

    public suspend fun generateAuthToken(endpoint: Url, region: String, expiration: Duration): String {
        val req = HttpRequest(HttpMethod.GET, endpoint)

        val creds = credentials
        val serv = service

        val config = AwsSigningConfig {
            credentials = creds
            this.region = region
            service = serv
            signingDate = Clock.System.now()
            expiresAfter = expiration
            signatureType = AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS
        }

        return DefaultAwsSigner.sign(req, config).output.url.toString().trimScheme()
    }
}

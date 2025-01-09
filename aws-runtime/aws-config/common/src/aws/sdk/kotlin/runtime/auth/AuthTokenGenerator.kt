/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSignatureType
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigner
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig.Companion.invoke
import aws.smithy.kotlin.runtime.auth.awssigning.DefaultAwsSigner
import aws.smithy.kotlin.runtime.http.HttpMethod
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.net.url.Url
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.util.ExpiringValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

// The default expiration value to use for [Credentials] when none is provided.
private val DEFAULT_CREDENTIALS_EXPIRATION = 10.minutes

/**
 * Generates an authentication token, which is a SigV4-signed URL with the HTTP scheme removed.
 * @param service The name of the service the token is being generated for
 * @param credentialsProvider The [CredentialsProvider] which will provide credentials to use when generating the auth token, defaults to [DefaultChainCredentialsProvider]
 * @param credentialsRefreshBuffer The amount of time before the resolved [Credentials] expire in which they are considered expired, defaults to 10 seconds.
 * @param signer The [AwsSigner] implementation to use when creating the authentication token, defaults to [DefaultAwsSigner]
 * @param clock The [Clock] implementation to use
 */
public class AuthTokenGenerator(
    public val service: String,
    public val credentialsProvider: CredentialsProvider = DefaultChainCredentialsProvider(),
    public val credentialsRefreshBuffer: Duration = 10.seconds,
    public val signer: AwsSigner = DefaultAwsSigner,
    public val clock: Clock = Clock.System
) {
    private lateinit var credentials: ExpiringValue<Credentials>

    private fun Url.trimScheme(): String = toString().removePrefix(scheme.protocolName).removePrefix("://")

    public suspend fun generateAuthToken(endpoint: Url, region: String, expiration: Duration): String {
        if (!::credentials.isInitialized || (credentials.expiresAt - clock.now()).absoluteValue <= credentialsRefreshBuffer) {
            val resolved = credentialsProvider.resolve()
            credentials = ExpiringValue(resolved, resolved.expiration ?: (clock.now() + DEFAULT_CREDENTIALS_EXPIRATION))
        }

        val req = HttpRequest(HttpMethod.GET, endpoint)

        val creds = credentials.value
        val serv = service

        val config = AwsSigningConfig {
            credentials = creds
            this.region = region
            service = serv
            signingDate = clock.now()
            expiresAfter = expiration
            signatureType = AwsSignatureType.HTTP_REQUEST_VIA_QUERY_PARAMS
        }

        return signer.sign(req, config).output.url.trimScheme()
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.io.use
import aws.smithy.kotlin.runtime.tracing.TraceSpan
import aws.smithy.kotlin.runtime.tracing.trace
import aws.smithy.kotlin.runtime.util.Platform

private const val PROVIDER_NAME = "Environment"

private val ACCESS_KEY_ID = AwsSdkSetting.AwsAccessKeyId.environmentVariable
private val SECRET_ACCESS_KEY = AwsSdkSetting.AwsSecretAccessKey.environmentVariable
private val SESSION_TOKEN = AwsSdkSetting.AwsSessionToken.environmentVariable

/**
 * A [CredentialsProvider] which reads from `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_SESSION_TOKEN`.
 */
public class EnvironmentCredentialsProvider
public constructor(private val getEnv: (String) -> String?) : CredentialsProvider {
    public constructor() : this(Platform::getenv)

    private fun requireEnv(variable: String): String =
        getEnv(variable) ?: throw ProviderConfigurationException("Missing value for environment variable `$variable`")

    override suspend fun getCredentials(traceSpan: TraceSpan): Credentials =
        traceSpan.child("Environment").use { childSpan ->
            childSpan.trace<EnvironmentCredentialsProvider> {
                "Attempting to load credentials from env vars $ACCESS_KEY_ID/$SECRET_ACCESS_KEY/$SESSION_TOKEN"
            }
            Credentials(
                accessKeyId = requireEnv(ACCESS_KEY_ID),
                secretAccessKey = requireEnv(SECRET_ACCESS_KEY),
                sessionToken = getEnv(SESSION_TOKEN),
                providerName = PROVIDER_NAME,
            )
        }
}

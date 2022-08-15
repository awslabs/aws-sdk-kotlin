/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.util.Platform

private const val PROVIDER_NAME = "Environment"

/**
 * A [CredentialsProvider] which reads from `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_SESSION_TOKEN`.
 */
public class EnvironmentCredentialsProvider
public constructor(private val getEnv: (String) -> String?) : CredentialsProvider {
    public constructor() : this(Platform::getenv)

    private fun requireEnv(variable: String): String =
        getEnv(variable) ?: throw ProviderConfigurationException("Missing value for environment variable `$variable`")

    override suspend fun getCredentials(): Credentials = Credentials(
        accessKeyId = requireEnv(AwsSdkSetting.AwsAccessKeyId.environmentVariable),
        secretAccessKey = requireEnv(AwsSdkSetting.AwsSecretAccessKey.environmentVariable),
        sessionToken = getEnv(AwsSdkSetting.AwsSessionToken.environmentVariable),
        providerName = PROVIDER_NAME,
    )
}

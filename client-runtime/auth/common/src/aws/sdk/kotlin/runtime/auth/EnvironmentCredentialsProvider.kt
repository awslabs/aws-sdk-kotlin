/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.runtime.ConfigurationException
import software.aws.clientrt.util.Platform

/**
 * A [CredentialsProvider] which reads from `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_SESSION_TOKEN`.
 */
public class EnvironmentCredentialsProvider
public constructor(private val getEnv: (String) -> String?) : CredentialsProvider {
    public companion object {
        internal const val ACCESS_KEY_ID: String = "AWS_ACCESS_KEY_ID"
        internal const val SECRET_ACCESS_KEY: String = "AWS_SECRET_ACCESS_KEY"
        internal const val SESSION_TOKEN: String = "AWS_SESSION_TOKEN"
    }

    public constructor() : this(Platform::getenv)

    private fun requireEnv(variable: String): String =
        getEnv(variable) ?: throw ConfigurationException("Unable to get value from environment variable $variable")

    override suspend fun getCredentials(): Credentials = Credentials(
        accessKeyId = requireEnv(ACCESS_KEY_ID),
        secretAccessKey = requireEnv(SECRET_ACCESS_KEY),
        sessionToken = getEnv(SESSION_TOKEN),
    )
}

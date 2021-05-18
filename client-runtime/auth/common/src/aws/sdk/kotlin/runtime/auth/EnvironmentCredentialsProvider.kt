/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.runtime.auth.exceptions.AuthenticationException
import software.aws.clientrt.util.Platform

/**
 * A [CredentialsProvider] which reads from `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, and `AWS_SESSION_TOKEN`.
 */
public class EnvironmentCredentialsProvider(private val getEnv: (String) -> String?) : CredentialsProvider {
    companion object {
        internal const val accessKeyId: String = "AWS_ACCESS_KEY_ID"
        internal const val secretAccessKey: String = "AWS_SECRET_ACCESS_KEY"
        internal const val sessionToken: String = "AWS_SESSION_TOKEN"
    }

    public constructor() : this(Platform::getenv)

    private fun requireEnv(name: String, variable: String): String =
        getEnv(variable) ?: throw AuthenticationException("Unable to get $name from environment variable $variable")

    override suspend fun getCredentials(): Credentials = Credentials(
        accessKeyId = requireEnv("access key ID", accessKeyId),
        secretAccessKey = requireEnv("secret access key", secretAccessKey),
        sessionToken = getEnv(sessionToken),
    )
}

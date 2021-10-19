/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.auth.credentials.Credentials
import aws.sdk.kotlin.crt.auth.credentials.build
import aws.sdk.kotlin.runtime.crt.SdkDefaultIO
import aws.sdk.kotlin.crt.auth.credentials.CredentialsProvider as CredentialsProviderCrt
import aws.sdk.kotlin.crt.auth.credentials.StsAssumeRoleCredentialsProvider as StsAssumeRoleCredentialsProviderCrt

/**
 * A provider that gets credentials from the STS assume role credential provider.
 *
 * @param credentialsProvider The underlying Credentials Provider to use for source credentials
 * @param roleArn The target role's ARN
 * @param sessionName The name to associate with the session
 * @param durationSeconds The number of seconds from authentication that the session is valid for
 */
public class StsAssumeRoleCredentialsProvider public constructor(
    credentialsProvider: CredentialsProvider,
    roleArn: String? = null,
    sessionName: String? = null,
    durationSeconds: Int? = null,
) : CrtCredentialsProvider {
    override val crtProvider: StsAssumeRoleCredentialsProviderCrt = StsAssumeRoleCredentialsProviderCrt.build {
        clientBootstrap = SdkDefaultIO.ClientBootstrap
        tlsContext = SdkDefaultIO.TlsContext
        this.credentialsProvider = adapt(credentialsProvider)
        this.roleArn = roleArn
        this.sessionName = sessionName
        this.durationSeconds = durationSeconds
    }
}

// Adapt SDK credential provider to CRT version
internal fun adapt(credentialsProvider: CredentialsProvider): CredentialsProviderCrt =
    object : CredentialsProviderCrt {
        override fun close() { }

        override suspend fun getCredentials(): Credentials {
            val credentials = credentialsProvider.getCredentials()
            return Credentials(credentials.accessKeyId, credentials.secretAccessKey, credentials.sessionToken)
        }

        override suspend fun waitForShutdown() { }
    }

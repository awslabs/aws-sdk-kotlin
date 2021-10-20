/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.auth.credentials.build
import aws.sdk.kotlin.runtime.crt.SdkDefaultIO
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
    roleArn: String,
    sessionName: String,
    durationSeconds: Int? = null,
) : CrtCredentialsProvider {
    override val crtProvider: StsAssumeRoleCredentialsProviderCrt = StsAssumeRoleCredentialsProviderCrt.build {
        clientBootstrap = SdkDefaultIO.ClientBootstrap
        tlsContext = SdkDefaultIO.TlsContext
        this.credentialsProvider = asCrt(credentialsProvider)
        this.roleArn = roleArn
        this.sessionName = sessionName
        this.durationSeconds = durationSeconds
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.crt.auth.credentials.build
import aws.sdk.kotlin.runtime.config.AwsSdkSetting
import aws.sdk.kotlin.runtime.config.resolve
import aws.sdk.kotlin.runtime.crt.SdkDefaultIO
import aws.smithy.kotlin.runtime.util.Platform
import aws.sdk.kotlin.crt.auth.credentials.ProfileCredentialsProvider as ProfileCredentialsProviderCrt

/**
 * A provider that gets credentials from a profile.
 * @param profileName The name of the profile to use (or `"default"` if none is specified).
 * @param configFileName The name of the config file to use. If none is specified, the default is `".aws/config"` on
 * Linux/Mac and`"%USERPROFILE%\.aws\config"` on Windows.
 * @param credentialsFileName The name of the credentials file to use. If none is specified, the default is
 * `".aws/credentials"` on Linux/Mac and `"%USERPROFILE%\.aws\credentials"` on Windows.
 */
public class ProfileCredentialsProvider public constructor(
    profileName: String? = null,
    configFileName: String? = null,
    credentialsFileName: String? = null,
) : CrtCredentialsProvider {
    override val crtProvider: ProfileCredentialsProviderCrt = ProfileCredentialsProviderCrt.build {
        clientBootstrap = SdkDefaultIO.ClientBootstrap
        tlsContext = SdkDefaultIO.TlsContext
        this.profileName = profileName ?: AwsSdkSetting.AwsProfile.resolve(Platform)
        this.configFileName = configFileName
        this.credentialsFileName = credentialsFileName
    }
}

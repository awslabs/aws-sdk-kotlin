/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth

import aws.sdk.kotlin.crt.SdkDefaultIO
import aws.sdk.kotlin.crt.auth.credentials.build
import aws.sdk.kotlin.crt.auth.credentials.ProfileCredentialsProvider as ProfileCredentialsProviderCrt

/**
 * A provider that gets credentials from a profile.
 * @param profileName The name of the profile to use (or `"default"` if none is specified).
 * @param configFileNameOverride The name of the config file to use. If none is specified, the default is
 * `".aws/config"` on Linux/Mac and`"%USERPROFILE%\.aws\config"` on Windows.
 * @param credentialsFileNameOverride The name of the credentials file to use. If none is specified, the default is
 * `".aws/credentials"` on Linux/Mac and `"%USERPROFILE%\.aws\credentials"` on Windows.
 */
public class ProfileCredentialsProvider public constructor(
    profileName: String? = null,
    configFileNameOverride: String? = null,
    credentialsFileNameOverride: String? = null,
) : CrtBasedCredentialsProvider {
    override val crtProvider = ProfileCredentialsProviderCrt.build {
        clientBootstrap = SdkDefaultIO.ClientBootstrap
        tlsContext = SdkDefaultIO.TlsContext
        this.profileName = profileName
        this.configFileNameOverride = configFileNameOverride
        this.credentialsFileNameOverride = credentialsFileNameOverride
    }
}

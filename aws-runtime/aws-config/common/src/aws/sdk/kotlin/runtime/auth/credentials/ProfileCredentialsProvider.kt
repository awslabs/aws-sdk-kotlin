/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.config.AwsSdkSetting

/**
 * A [CredentialsProvider] that gets credentials from a profile in `~/.aws/config` or the shared credentials
 * file `~/.aws/credentials`. The locations of these files are configurable via environment or system property on
 * the JVM (see [AwsSdkSetting.AwsConfigFile] and [AwsSdkSetting.AwsSharedCredentialsFile]).
 *
 * NOTE: This provider does not implement any caching. It will reload and reparse the profile from the file system
 * when called. Use [CachedCredentialsProvider] to decorate the profile provider to get caching behavior.
 *
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
) : CredentialsProvider {

    override suspend fun getCredentials(): Credentials {
        TODO("Not yet implemented")
    }
}

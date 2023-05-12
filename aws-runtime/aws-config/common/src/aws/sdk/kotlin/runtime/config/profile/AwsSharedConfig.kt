/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * Represents shared configuration (profiles, SSO sessions, credentials, etc)
 */
@InternalSdkApi
public class AwsSharedConfig internal constructor(
    private val sections: TypedSectionMap,
    private val source: AwsConfigurationSource,
) {

    /**
     * Map of profile names to profile.
     */
    public val profiles: Map<String, AwsProfile>
        get() = sections[ConfigSectionType.PROFILE] ?: emptyMap()

    /**
     * Map of SSO session name to SsoSessionConfig
     */
    public val ssoSessions: Map<String, SsoSessionConfig>
        get() = sections[ConfigSectionType.SSO_SESSION] ?: emptyMap()

    /**
     * Resolve the active profile or the default profile if none is defined
     */
    public val activeProfile: AwsProfile
        get() = profiles[source.profile] ?: AwsProfile(source.profile, emptyMap())
}

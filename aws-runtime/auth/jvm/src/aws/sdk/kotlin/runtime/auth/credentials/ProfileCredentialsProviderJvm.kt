/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.sdk.kotlin.runtime.AwsSdkSetting

/**
 * Attempts to load profile name using the following priority,
 * if neither are available, profileName will remain blank.
 * 1. JVM System Property
 * 2. Environment Variable
 */
internal actual fun loadProfileName(): String? {
    val profileSysPropOverride = System.getProperty(AwsSdkSetting.AwsProfile.jvmProperty, "")
    return profileSysPropOverride.ifEmpty {
        System.getenv().getOrDefault(AwsSdkSetting.AwsProfile.environmentVariable, null)
    }
}

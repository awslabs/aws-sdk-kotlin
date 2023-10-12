/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.config.utils

import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.PlatformProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.File

internal fun mockPlatform(
    pathSegment: String,
    awsProfileEnv: String? = null,
    awsConfigFileEnv: String? = null,
    homeEnv: String? = null,
    awsSharedCredentialsFileEnv: String? = null,
    awsSdkUserAgentAppIdEnv: String? = null,
    homeProp: String? = null,
    os: OperatingSystem,
): PlatformProvider {
    val testPlatform = mockk<PlatformProvider>()
    val envKeyParam = slot<String>()
    val propKeyParam = slot<String>()
    val filePath = slot<String>()

    every { testPlatform.filePathSeparator } returns pathSegment
    every { testPlatform.getenv(capture(envKeyParam)) } answers {
        when (envKeyParam.captured) {
            "AWS_PROFILE" -> awsProfileEnv
            "AWS_CONFIG_FILE" -> awsConfigFileEnv
            "HOME" -> homeEnv
            "AWS_SHARED_CREDENTIALS_FILE" -> awsSharedCredentialsFileEnv
            "AWS_SDK_UA_APP_ID" -> awsSdkUserAgentAppIdEnv
            else -> error(envKeyParam.captured)
        }
    }
    every { testPlatform.getProperty(capture(propKeyParam)) } answers {
        if (propKeyParam.captured == "user.home") homeProp else null
    }
    every { testPlatform.osInfo() } returns os
    coEvery {
        testPlatform.readFileOrNull(capture(filePath))
    } answers {
        if (awsConfigFileEnv != null) {
            val file = if (filePath.captured.endsWith("config")) {
                File(awsConfigFileEnv)
            } else {
                File(awsSharedCredentialsFileEnv)
            }

            if (file.exists()) file.readBytes() else null
        } else {
            null
        }
    }

    return testPlatform
}

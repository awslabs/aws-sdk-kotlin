/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.integration.GradleBuildSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration

/**
 * Integration that registers custom gradle settings
 */
class GradleSettings : KotlinIntegration {
    override val customBuildSettings: GradleBuildSettings?
        get() = GradleBuildSettings().apply {
            experimentalAnnotations += listOf(
                "software.aws.clientrt.util.InternalAPI",
                "aws.sdk.kotlin.runtime.InternalSdkApi"
            )
        }
}

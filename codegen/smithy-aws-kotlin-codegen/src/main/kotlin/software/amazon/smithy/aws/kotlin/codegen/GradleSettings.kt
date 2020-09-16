/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.amazon.smithy.aws.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.integration.GradleBuildSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration

/**
 * Integration that registers custom gradle settings
 */
class GradleSettings : KotlinIntegration {
    override val customBuildSettings: GradleBuildSettings?
        get() = GradleBuildSettings().apply {
            experimentalAnnotations += "software.aws.clientrt.util.InternalAPI"
        }
}

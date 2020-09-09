/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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

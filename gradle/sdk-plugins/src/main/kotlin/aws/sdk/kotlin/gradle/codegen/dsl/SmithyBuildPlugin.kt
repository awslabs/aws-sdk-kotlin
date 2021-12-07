/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle.codegen.dsl

import software.amazon.smithy.model.node.ToNode

interface SmithyBuildPlugin : ToNode {
    /**
     * The name of the build plugin (e.g. `kotlin-codegen`). This is used when generating
     * the projection settings for the plugin
     */
    val pluginName: String
}

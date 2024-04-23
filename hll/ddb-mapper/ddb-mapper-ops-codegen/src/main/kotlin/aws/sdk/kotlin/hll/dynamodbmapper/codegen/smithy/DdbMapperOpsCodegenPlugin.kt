/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.smithy

import software.amazon.smithy.build.PluginContext
import software.amazon.smithy.build.SmithyBuildPlugin

class DdbMapperOpsCodegenPlugin : SmithyBuildPlugin {
    override fun getName() = "ddb-mapper-ops-codegen"

    override fun execute(context: PluginContext?) {
        requireNotNull(context) { "Cannot codegen with a null context" }
        TODO("Not yet implemented")
    }
}
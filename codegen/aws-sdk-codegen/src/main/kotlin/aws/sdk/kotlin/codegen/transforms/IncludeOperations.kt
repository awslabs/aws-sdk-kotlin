/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.transforms

import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.build.transforms.ConfigurableProjectionTransformer
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape

/**
 * A smithy build [transform](https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#transforms)
 * that filters out operations not included in the `operations` list of shape IDs
 */
class IncludeOperations : ConfigurableProjectionTransformer<IncludeOperations.Config>() {

    class Config {
        var operations: Set<String> = emptySet()
    }

    override fun getName(): String = "awsSdkKotlinIncludeOperations"
    override fun getConfigType(): Class<Config> = Config::class.java

    override fun transformWithConfig(context: TransformContext, config: Config): Model {
        check(config.operations.isNotEmpty()) { "no operations provided to IncludeOperations transform!" }
        return context.transformer.filterShapes(context.model) { shape ->
            when (shape) {
                is OperationShape -> shape.id.toString() in config.operations
                else -> true
            }
        }
    }
}

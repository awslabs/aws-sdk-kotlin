/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.util

/**
 * Named constants for various mapper runtime packages
 */
object Pkg {
    object Hl {
        val Base = "aws.sdk.kotlin.hll.dynamodbmapper"
        val Items = "$Base.items"
        val Model = "$Base.model"
        val Ops = "$Base.operations"
        val PipelineImpl = "$Base.pipeline.internal"
    }

    object Kotlin {
        val Base = "kotlin"
        val Collections = "$Base.collections"
    }

    object Ll {
        val Base = "aws.sdk.kotlin.services.dynamodb"
        val Model = "$Base.model"
    }
}

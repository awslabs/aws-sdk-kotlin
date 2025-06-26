/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

/**
 * Represents an operation constant with its Smithy shape ID.
 * This class encapsulates the shape ID of an AWS operation for use in custom SDK generation.
 */
data class OperationConstant(val shapeId: String) {
    override fun toString(): String = shapeId
}

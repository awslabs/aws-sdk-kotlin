/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Defines [AttributeKey] instances that relate to the data model of low-level to high-level codegen
 */
internal object MapperAttributes {
    /**
     * For a given [Operation], this attribute key contains relevant pagination members (if applicable) in the request
     * and response
     */
    val PaginationInfo: AttributeKey<PaginationMembers> = AttributeKey("aws.sdk.kotlin.ddbmapper#PaginationInfo")
}

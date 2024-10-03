/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.operations.model

import aws.sdk.kotlin.hll.codegen.model.Member
import aws.sdk.kotlin.hll.codegen.model.ModelParsingPlugin
import aws.sdk.kotlin.hll.codegen.model.Operation
import aws.sdk.kotlin.hll.codegen.util.plus

/**
 * Identifies the [Member] instances of an operation's request and response which control pagination
 * @param inputToken The field for passing a pagination token into a request
 * @param outputToken The field for receiving a pagination token from a request
 * @param limit The field for limiting the number of returned results
 * @param items The field for getting the low-level items from each page of results
 */
internal data class PaginationMembers(
    val inputToken: Member,
    val outputToken: Member,
    val limit: Member,
    val items: Member,
) {
    internal companion object {
        fun forOperationOrNull(operation: Operation): PaginationMembers? {
            val inputToken = operation.request.members.find { it.name == "exclusiveStartKey" } ?: return null
            val outputToken = operation.response.members.find { it.name == "lastEvaluatedKey" } ?: return null
            val limit = operation.request.members.find { it.name == "limit" } ?: return null
            val items = operation.response.members.find { it.name == "items" } ?: return null

            return PaginationMembers(inputToken, outputToken, limit, items)
        }
    }
}

/**
 * Gets the [PaginationMembers] for an operation, if applicable. If the operation does not support pagination, this
 * property returns `null`.
 */
internal val Operation.paginationInfo: PaginationMembers?
    get() = attributes.getOrNull(MapperAttributes.PaginationInfo)

/**
 * A codegen plugin that adds DDB-specific pagination info to operations
 */
internal class DdbPaginationPlugin : ModelParsingPlugin {
    override fun postProcessOperation(operation: Operation): Operation {
        val paginationMembers = PaginationMembers.forOperationOrNull(operation) ?: return operation
        val newAttributes = operation.attributes + (MapperAttributes.PaginationInfo to paginationMembers)
        return operation.copy(attributes = newAttributes)
    }
}

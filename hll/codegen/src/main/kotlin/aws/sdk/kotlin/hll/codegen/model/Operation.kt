/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.codegen.model

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.capitalizeFirstChar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.plus
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Describes a service operation (i.e., API method)
 * @param methodName The name of the operation as a code-suitable method name. For example, `getItem` is a suitable
 * method name in Kotlin, whereas `GetItem` is not (improperly cased) nor is `get item` (contains a space).
 * @param request The [Structure] for requests/inputs to this operation
 * @param response The [Structure] for responses/output from this operation
 * @param attributes An [Attributes] collection for associating typed attributes with this operation
 */
data class Operation(
    val methodName: String,
    val request: Structure,
    val response: Structure,
    val attributes: Attributes = emptyAttributes(),
) {
    /**
     * The capitalized name of this operation's [methodName]. For example, if [methodName] is `getItem` then [name]
     * would be `GetItem`.
     */
    val name = methodName.capitalizeFirstChar // e.g., "GetItem" vs "getItem"

    companion object {
        /**
         * Derive an [Operation] from a [KSFunctionDeclaration]
         */
        fun from(declaration: KSFunctionDeclaration) = Operation(
            methodName = declaration.simpleName.getShortName(),
            request = Structure.from(declaration.parameters.single().type),
            response = Structure.from(declaration.returnType!!),
        )
    }
}

/**
 * Gets the low-level [Operation] equivalent for this high-level operation
 */
val Operation.lowLevel: Operation
    get() = attributes[ModelAttributes.LowLevelOperation]

/**
 * Derives a high-level [Operation] equivalent for this low-level operation
 * @param pkg The Kotlin package to use for the high-level operation's request and response structures
 */
fun Operation.toHighLevel(pkg: String): Operation {
    val llOperation = this@toHighLevel
    val hlRequest = llOperation.request.toHighLevel(pkg)
    val hlResponse = llOperation.response.toHighLevel(pkg)
    val hlAttributes = llOperation.attributes + (ModelAttributes.LowLevelOperation to llOperation)
    return Operation(llOperation.methodName, hlRequest, hlResponse, hlAttributes)
}

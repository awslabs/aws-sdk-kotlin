/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.capitalizeFirstChar
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.util.plus
import aws.smithy.kotlin.runtime.collections.Attributes
import aws.smithy.kotlin.runtime.collections.emptyAttributes
import aws.smithy.kotlin.runtime.collections.get
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

internal data class Operation(
    val methodName: String,
    val request: Structure,
    val response: Structure,
    val attributes: Attributes = emptyAttributes(),
) {
    val name = methodName.capitalizeFirstChar // e.g., "GetItem" vs "getItem"

    companion object {
        fun from(declaration: KSFunctionDeclaration) = Operation(
            methodName = declaration.simpleName.getShortName(),
            request = Structure.from(declaration.parameters.single().type),
            response = Structure.from(declaration.returnType!!),
        )
    }
}

internal val Operation.lowLevel: Operation
    get() = attributes[ModelAttributes.LowLevelOperation]

internal fun Operation.toHighLevel(pkg: String): Operation {
    val llOperation = this@toHighLevel
    val hlRequest = llOperation.request.toHighLevel(pkg)
    val hlResponse = llOperation.response.toHighLevel(pkg)
    val hlAttributes = llOperation.attributes + (ModelAttributes.LowLevelOperation to llOperation)
    return Operation(llOperation.methodName, hlRequest, hlResponse, hlAttributes)
}

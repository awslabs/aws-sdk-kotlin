/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.TitleTrait

/**
 * Metadata about a discovered AWS service.
 */
data class ServiceMetadata(
    val serviceName: String,
    val sdkId: String,
    val namespace: String,
    val title: String,
    val operations: List<OperationMetadata>
)

/**
 * Metadata about an operation within a service.
 */
data class OperationMetadata(
    val name: String,
    val shapeId: String
)

/**
 * Discover AWS services and their operations from the current model.
 */
fun discoverServiceMetadata(ctx: CodegenContext): List<ServiceMetadata> {
    val services = mutableListOf<ServiceMetadata>()
    
    ctx.model.shapes(ServiceShape::class.java).forEach { serviceShape ->
        val serviceTrait = serviceShape.getTrait<ServiceTrait>()
        if (serviceTrait != null) {
            val serviceMetadata = extractServiceMetadata(ctx.model, serviceShape, serviceTrait)
            services.add(serviceMetadata)
        }
    }
    
    return services.sortedBy { it.serviceName }
}

/**
 * Extract metadata from a service shape.
 */
private fun extractServiceMetadata(
    model: Model, 
    serviceShape: ServiceShape, 
    serviceTrait: ServiceTrait
): ServiceMetadata {
    val serviceName = serviceTrait.sdkId.lowercase().replace(" ", "").replace("-", "")
    val namespace = serviceShape.id.namespace
    val title = serviceShape.getTrait<TitleTrait>()?.value ?: serviceTrait.sdkId
    
    val operations = serviceShape.allOperations.map { operationId ->
        val operationShape = model.expectShape<OperationShape>(operationId)
        OperationMetadata(
            name = operationShape.id.name,
            shapeId = operationShape.id.toString()
        )
    }.sortedBy { it.name }
    
    return ServiceMetadata(
        serviceName = serviceName,
        sdkId = serviceTrait.sdkId,
        namespace = namespace,
        title = title,
        operations = operations
    )
}

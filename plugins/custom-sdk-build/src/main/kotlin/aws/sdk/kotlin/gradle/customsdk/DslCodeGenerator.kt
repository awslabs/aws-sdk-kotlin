/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter

/**
 * Generates DSL code for service configurations and operation constants.
 */
object DslCodeGenerator {
    
    fun generateServiceConfigurations(writer: KotlinWriter, services: List<ServiceMetadata>) {
        writer.write("package aws.sdk.kotlin.gradle.customsdk.dsl\n\n")
        writer.write("data class OperationConstant(val shapeId: String)\n\n")
        
        services.forEach { service ->
            generateServiceConfigurationClass(writer, service)
        }
    }
    
    fun generateOperationConstants(writer: KotlinWriter, services: List<ServiceMetadata>) {
        writer.write("package aws.sdk.kotlin.gradle.customsdk.dsl\n\n")
        
        services.forEach { service ->
            generateServiceOperationConstants(writer, service)
        }
    }
    
    private fun generateServiceConfigurationClass(writer: KotlinWriter, service: ServiceMetadata) {
        val className = "${service.serviceName.replaceFirstChar { it.uppercase() }}ServiceConfiguration"
        val operationObjectName = "${service.serviceName.replaceFirstChar { it.uppercase() }}Operation"
        
        writer.write("""
            /**
             * Configuration for ${service.title} (${service.sdkId}).
             */
            class $className {
                internal val selectedOperations = mutableListOf<OperationConstant>()
                
                fun operations(vararg operations: $operationObjectName) {
                    selectedOperations.addAll(operations.map { it.constant })
                }
            }
            
        """.trimIndent())
    }
    
    private fun generateServiceOperationConstants(writer: KotlinWriter, service: ServiceMetadata) {
        val enumName = "${service.serviceName.replaceFirstChar { it.uppercase() }}Operation"
        
        writer.write("enum class $enumName(val constant: OperationConstant) {\n")
        
        service.operations.forEachIndexed { index, operation ->
            val isLast = index == service.operations.size - 1
            val comma = if (isLast) "" else ","
            writer.write("    ${operation.name}(OperationConstant(\"${operation.shapeId}\"))$comma\n")
        }
        
        writer.write("}\n\n")
    }
}

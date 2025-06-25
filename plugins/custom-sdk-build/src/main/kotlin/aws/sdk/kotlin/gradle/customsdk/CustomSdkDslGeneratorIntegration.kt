/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.model.Model

/**
 * KotlinIntegration that generates DSL code for the custom SDK build plugin.
 */
class CustomSdkDslGeneratorIntegration : KotlinIntegration {
    
    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        if (!isPluginBuild(ctx)) return
        
        generateServiceDslClasses(ctx, delegator)
        generateOperationConstants(ctx, delegator)
    }
    
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        return isPluginBuild(settings)
    }
    
    private fun isPluginBuild(ctx: CodegenContext): Boolean = isPluginBuild(ctx.settings)
    
    private fun isPluginBuild(settings: KotlinSettings): Boolean {
        return settings.pkg.name.contains("custom-sdk-build") || 
               settings.pkg.name.contains("customsdk")
    }
    
    private fun generateServiceDslClasses(ctx: CodegenContext, delegator: KotlinDelegator) {
        val serviceMetadata = discoverServiceMetadata(ctx)
        val namespace = "aws.sdk.kotlin.gradle.customsdk.dsl"
        
        delegator.useFileWriter("ServiceConfigurations.kt", namespace) { writer ->
            writer.write("package $namespace\n\n")
            writer.write("data class OperationConstant(val shapeId: String)\n\n")
            
            serviceMetadata.forEach { service ->
                generateServiceConfigurationClass(writer, service)
            }
        }
    }
    
    private fun generateOperationConstants(ctx: CodegenContext, delegator: KotlinDelegator) {
        val serviceMetadata = discoverServiceMetadata(ctx)
        val namespace = "aws.sdk.kotlin.gradle.customsdk.dsl"
        
        delegator.useFileWriter("OperationConstants.kt", namespace) { writer ->
            writer.write("package $namespace\n\n")
            
            serviceMetadata.forEach { service ->
                generateOperationConstants(writer, service)
            }
        }
    }
    
    private fun generateServiceConfigurationClass(
        writer: software.amazon.smithy.kotlin.codegen.core.KotlinWriter, 
        service: ServiceMetadata
    ) {
        val className = "${service.serviceName.replaceFirstChar { it.uppercase() }}ServiceConfiguration"
        
        writer.write("""
            class $className {
                internal val selectedOperations = mutableListOf<OperationConstant>()
                
                fun operations(vararg operations: ${service.serviceName.replaceFirstChar { it.uppercase() }}Operation) {
                    selectedOperations.addAll(operations)
                }
            }
            
        """.trimIndent())
    }
    
    private fun generateOperationConstants(
        writer: software.amazon.smithy.kotlin.codegen.core.KotlinWriter, 
        service: ServiceMetadata
    ) {
        val objectName = "${service.serviceName.replaceFirstChar { it.uppercase() }}Operation"
        
        writer.write("object $objectName {\n")
        
        service.operations.forEach { operation ->
            writer.write("    val ${operation.name} = OperationConstant(\"${operation.shapeId}\")\n")
        }
        
        writer.write("}\n\n")
    }
}

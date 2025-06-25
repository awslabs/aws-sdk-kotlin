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
        
        // Generate service configurations
        delegator.useFileWriter("ServiceConfigurations.kt", namespace) { writer ->
            DslCodeGenerator.generateServiceConfigurations(writer, serviceMetadata)
        }
        
        // Generate main DSL extension
        delegator.useFileWriter("CustomSdkBuildExtension.kt", namespace) { writer ->
            MainDslGenerator.generateMainDslExtension(writer, serviceMetadata)
        }
    }
    
    private fun generateOperationConstants(ctx: CodegenContext, delegator: KotlinDelegator) {
        val serviceMetadata = discoverServiceMetadata(ctx)
        val namespace = "aws.sdk.kotlin.gradle.customsdk.dsl"
        
        delegator.useFileWriter("OperationConstants.kt", namespace) { writer ->
            DslCodeGenerator.generateOperationConstants(writer, serviceMetadata)
        }
    }
}

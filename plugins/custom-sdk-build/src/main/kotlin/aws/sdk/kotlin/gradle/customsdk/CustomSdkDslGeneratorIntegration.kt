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
 * 
 * This integration runs during the plugin's own build process to generate
 * typed service configuration classes and operation constants that provide
 * a type-safe DSL for users to select AWS services and operations.
 */
class CustomSdkDslGeneratorIntegration : KotlinIntegration {
    
    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        // Only generate DSL for plugin build, not regular service builds
        if (!isPluginBuild(ctx)) {
            return
        }
        
        // Generate service DSL classes and operation constants
        generateServiceDslClasses(ctx, delegator)
        generateOperationConstants(ctx, delegator)
    }
    
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        // Enable for plugin DSL generation builds
        return isPluginBuild(settings)
    }
    
    /**
     * Detect if this is a plugin DSL generation build vs regular service build.
     * We can identify this by checking if the package name contains our plugin identifier.
     */
    private fun isPluginBuild(ctx: CodegenContext): Boolean {
        return isPluginBuild(ctx.settings)
    }
    
    private fun isPluginBuild(settings: KotlinSettings): Boolean {
        return settings.pkg.name.contains("custom-sdk-build") || 
               settings.pkg.name.contains("customsdk")
    }
    
    /**
     * Generate service configuration classes for discovered AWS services.
     */
    private fun generateServiceDslClasses(ctx: CodegenContext, delegator: KotlinDelegator) {
        // For now, create a placeholder file to establish the structure
        // Service discovery will be implemented in the next prompt
        val namespace = "aws.sdk.kotlin.gradle.customsdk.dsl"
        delegator.useFileWriter("ServiceDslPlaceholder.kt", namespace) { writer ->
            writer.write("""
                /*
                 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
                 * SPDX-License-Identifier: Apache-2.0
                 */
                package $namespace
                
                /**
                 * Placeholder for service DSL classes.
                 * Service discovery and DSL generation will be implemented in subsequent steps.
                 */
                internal object ServiceDslPlaceholder {
                    // Service DSL classes will be generated here
                }
            """.trimIndent())
        }
    }
    
    /**
     * Generate operation constants for discovered AWS services.
     */
    private fun generateOperationConstants(ctx: CodegenContext, delegator: KotlinDelegator) {
        // For now, create a placeholder file to establish the structure
        // Operation extraction will be implemented in the next prompt
        val namespace = "aws.sdk.kotlin.gradle.customsdk.dsl"
        delegator.useFileWriter("OperationConstantsPlaceholder.kt", namespace) { writer ->
            writer.write("""
                /*
                 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
                 * SPDX-License-Identifier: Apache-2.0
                 */
                package $namespace
                
                /**
                 * Placeholder for operation constants.
                 * Operation extraction and constant generation will be implemented in subsequent steps.
                 */
                internal object OperationConstantsPlaceholder {
                    // Operation constants will be generated here
                }
            """.trimIndent())
        }
    }
}

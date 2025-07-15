/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen

import aws.sdk.kotlin.codegen.model.traits.CustomSdkBuildDsl
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.utils.toCamelCase
import software.amazon.smithy.kotlin.codegen.utils.toPascalCase
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.TopDownIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ToShapeId

/**
 * Generates DSL operation constants for the AWS Custom SDK Build plugin.
 * 
 * This integration runs during the existing ./gradlew bootstrap process and generates
 * type-safe operation constants (e.g., S3Operations.GetObject) that can be used
 * in the plugin's DSL configuration.
 * 
 * The constants are generated per-service and copied to the plugin's resources
 * directory for distribution.
 */
class DslConstantsIntegration : KotlinIntegration {
    companion object {
        fun annotatedOperations(model: Model, service: ToShapeId) = TopDownIndex
            .of(model)
            .getContainedOperations(service)
            .filter { it.hasTrait<CustomSdkBuildDsl>() }
            .sortedBy { it.id.name }
    }

    override val order: Byte = 127 // Run last to ensure all other processing is complete

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        annotatedOperations(model, settings.service).isNotEmpty()

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val serviceName = getServiceName(ctx.settings.sdkId)
        val operations = annotatedOperations(ctx.model, ctx.settings.service)

        // Generate the DSL file
        generateOperationDsl(delegator, serviceName, operations)
    }
    
    /**
     * Generate the service name using the same logic as service client generation.
     * This ensures consistency between client names (e.g., "LambdaClient") and 
     * constants names (e.g., "LambdaOperations").
     */
    private fun getServiceName(sdkId: String): String {
        return sdkId.sanitizeClientName().toPascalCase()
    }
    
    /**
     * Sanitize the service name by removing common suffixes, following the same
     * logic as the service client naming in KotlinSymbolProvider.
     */
    private fun String.sanitizeClientName(): String =
        replace(Regex("(API|Client|Service)$", setOf(RegexOption.IGNORE_CASE)), "")
    
    /**
     * Generate operation DSL (i.e., extension methods and operations enum) for a specific service
     */
    private fun generateOperationDsl(
        delegator: KotlinDelegator,
        serviceName: String,
        operations: List<OperationShape>
    ) {
        val dslClassName = "${serviceName}Dsl"
        val fileName = "$dslClassName.kt"
        val operationClassName = "${serviceName}Operation"

        delegator.useFileWriter(fileName, "aws.sdk.kotlin.gradle.customsdk") { writer ->
            writer.dokka("The operations available in $serviceName")
            writer.withBlock(
                "public class #L internal constructor(extension: #T) : #T(extension, #S) {",
                "}",
                dslClassName,
                AwsPluginTypes.CustomSdkBuild.AwsCustomSdkBuildExtension,
                AwsPluginTypes.CustomSdkBuild.ServiceDslBase,
                serviceName,
            ) {
                dokka("Denotes an operation available from $serviceName")
                withBlock("public sealed interface #L {", "}", operationClassName) {
                    dokka("The name of this operation")
                    write("public val name: String")
                }

                write("")
                dokka("Adds the given operation to the custom SDK client")
                write("public operator fun #L.unaryPlus() = addOperation(name)", operationClassName)

                operations.forEach { op ->
                    val opName = op.id.name

                    write("")
                    dokka("The $opName operation of $serviceName")
                    write("public object #1L : #2L { override val name = #1S }", opName, operationClassName)
                }
            }

            writer.write("")
            writer.dokka("Includes a $serviceName client in the build")
            writer.write(
                "public fun #1T.#2L(block: #3L.() -> Unit) = #3L(this).apply(block).updateExtension()",
                AwsPluginTypes.CustomSdkBuild.AwsCustomSdkBuildExtension,
                serviceName.toCamelCase(),
                dslClassName,
            )
        }
    }
}

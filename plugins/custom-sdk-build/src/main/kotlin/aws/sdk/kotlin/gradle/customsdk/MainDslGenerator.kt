/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import software.amazon.smithy.kotlin.codegen.core.KotlinWriter

object MainDslGenerator {
    
    fun generateMainDslExtension(writer: KotlinWriter, services: List<ServiceMetadata>) {
        writer.write("package aws.sdk.kotlin.gradle.customsdk.dsl\n\n")
        writer.write("import org.gradle.api.Project\n\n")
        
        writer.write("open class CustomSdkBuildExtension(private val project: Project) {\n")
        writer.write("    private val serviceConfigurations = mutableMapOf<String, Any>()\n\n")
        
        // Generate a few sample service methods to demonstrate the pattern
        val sampleServices = services.take(3) // Limit to avoid too much generated code
        sampleServices.forEach { service ->
            val methodName = service.serviceName.lowercase()
            val configClassName = "${service.serviceName.replaceFirstChar { it.uppercase() }}ServiceConfiguration"
            
            writer.write("    fun $methodName(configure: $configClassName.() -> Unit) {\n")
            writer.write("        serviceConfigurations[\"$methodName\"] = $configClassName().apply(configure)\n")
            writer.write("    }\n\n")
        }
        
        writer.write("    internal fun getSelectedOperations(): Map<String, List<String>> = emptyMap()\n")
        writer.write("}\n")
    }
}

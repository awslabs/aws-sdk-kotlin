/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.utils.CodeWriter

/**
 * Integration that generates custom gradle build files
 */
class GradleGenerator : KotlinIntegration {
    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) = generateGradleBuild(ctx, delegator)

    /**
     * Generate a customized version of `build.gradle.kts` for AWS services
     *
     * Services belong to the `aws-sdk-kotlin` build which configures services via `subprojects {}`, thus the generated
     * gradle build needs to be stripped down.
     */
    private fun generateGradleBuild(ctx: CodegenContext, delegator: KotlinDelegator) {
        val writer = CodeWriter().apply {
            trimBlankLines()
            trimTrailingSpaces()
            setIndentText("    ")
        }

        val dependencies = delegator.dependencies.mapNotNull { it.properties["dependency"] as? KotlinDependency }.distinct()

        writer.write("\n")

        if (ctx.settings.moduleDescription.isNotEmpty()) {
            writer.write("description = \$S", ctx.settings.moduleDescription)
        }

        writer.withBlock("\ndependencies {", "}\n") {
            val orderedDependencies = dependencies.sortedWith(compareBy({ it.config }, { it.artifact }))
            for (dependency in orderedDependencies) {
                write(dependency.dependencyNotation())
            }
        }

        val contents = writer.toString()
        delegator.fileManifest.writeFile("build.gradle.kts", contents)
    }
}

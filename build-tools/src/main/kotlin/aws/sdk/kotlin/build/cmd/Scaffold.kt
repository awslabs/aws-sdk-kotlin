/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build.cmd

import kotlinx.cli.*
import kotlinx.cli.ExperimentalCli
import software.amazon.smithy.model.Model
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Paths

// TODO - process documentation and title from the model? e.g.
// "smithy.api#documentation": "<fullname>Amazon API Gateway</fullname>\n        <p>Amazon API Gateway helps developers deliver robust, secure, and scalable mobile and web application back ends. API Gateway allows developers to securely connect mobile and web applications to APIs that run on AWS Lambda, Amazon EC2, or other publicly addressable web services that are hosted outside of AWS.</p>",
// "smithy.api#title": "Amazon API Gateway"

/**
 * Create service project scaffolding
 *
 * Usage:
 *
 * ```
 * ./gradlew -q :build-tools:run --args="scaffold -m PATH_TO_MODEL_FILE"
 * ```
 *
 * Description:
 *
 * By default smithy-kotlin codegen spits out a `build.gradle.kts` file with the dependencies needed by the
 * model/service client. This works well for single projects or perhaps a few but AWS SDK's have some additional
 * complexities to deal with:
 *
 * 1. automation - There are 250+ AWS services and models change frequently and new models can be introduced automatically.
 * 2. build maintenance - due to the large number of services it would be impractical or at the very least a poor experience
 * to dynamically configure that many projects on the fly.
 * 3. customizations - Many services require customizations which requires mixing in additional source code.
 *
 * Instead of relying on the build file spit out by codegen this tool creates a new project directory and build
 * file (`build.gradle.kts`) by processing the model. The build file is meant to be checked into the repository allowing
 * for the overall build to statically determine the set of services and not configure them on demand.
 *
 * By controlling the build file completely we can handle more complex setups outside of codegen which are only likely
 * to apply to aws-sdk-kotlin. The only thing we lose is the automated processing of dependencies but the set of
 * dependencies needed can be resolved either statically or via simple model traversal.
 */
@OptIn(ExperimentalCli::class)
class Scaffold : Subcommand("scaffold", "create a new service client project/build") {
    val model: String by option(ArgType.String, "model", "m", "The path to the model file").required()
    val outputDir: String? by option(ArgType.String, "output-dir", "o", "the parent output directory to create the scaffolding")
    val overwrite: Boolean by option(ArgType.Boolean, "overwrite", description = "overwrite an existing project build if one exists").default(false)

    override fun execute() {
        // commands are always started in the root project directory
        val cwd = Paths.get("").toAbsolutePath()
        val modelFile = File(Paths.get(model).toAbsolutePath().toString())
        val serviceId = getServiceDirName(modelFile)

        val sdkDir = if (outputDir != null) {
            Paths.get(outputDir)
        } else {
            cwd.resolve("services/$serviceId")
        }
        val buildFile = sdkDir.resolve("build.gradle.kts").toFile()

        if (buildFile.exists() && !overwrite) {
            println("$buildFile already exists, nothing to do")
            kotlin.system.exitProcess(0)
        }

        println("creating project scaffold for new service: $serviceId")

        try {
            sdkDir.toFile().mkdirs()
            FileWriter(buildFile.absolutePath).use { fw ->
                fw.write(buildFileTemplate(modelFile))
            }
            println("new project created in: $sdkDir")
        } catch (e: IOException) {
            e.printStackTrace()
            kotlin.system.exitProcess(-1)
        }
    }
}

private fun buildFileTemplate(modelFile: File): String {
    return """
    plugins {
        kotlin("jvm")
    }
    
    val model: String = "${modelFile.name}"

    """.trimIndent()
}

private fun getServiceDirName(modelFile: File): String {
    // discover the model file and use the sdkId to derive the name for the project
    val model = Model.assembler().addImport(modelFile.absolutePath).assemble().result.get()
    val services = model.getShapesWithTrait(software.amazon.smithy.aws.traits.ServiceTrait::class.java).toList()
    require(services.size == 1) { "Expected one service per aws model, but found ${services.size} in ${modelFile.absolutePath}: ${services.map { it.id }}" }
    val service = services.first()
    val serviceApi = service.expectTrait(software.amazon.smithy.aws.traits.ServiceTrait::class.java)
    return serviceApi.sdkId.split(" ").map {
        it.toLowerCase()
    }.joinToString("")
}

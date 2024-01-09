/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk.tasks

import aws.sdk.kotlin.gradle.sdk.PackageManifest
import aws.sdk.kotlin.gradle.sdk.PackageMetadata
import aws.sdk.kotlin.gradle.sdk.validate
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Task to update the package manifest which is used by the bootstrap process to generate service clients.
 * New services are required to be scaffolded
 */
abstract class Scaffold : DefaultTask() {

    @get:Option(option = "model-file", description = "the path to the model file")
    @get:InputFile
    public abstract val modelFile: RegularFileProperty

    @OptIn(ExperimentalSerializationApi::class)
    @TaskAction
    fun updatePackageManifest() {
        val manifestFile = project.file("packages.json")
        val json = Json { prettyPrint = true }

        val manifest = if (manifestFile.exists()) {
            val manifest = json.decodeFromStream<PackageManifest>(manifestFile.inputStream())
            manifest.validate()
            manifest
        } else {
            PackageManifest(emptyList())
        }

        val model = Model.assembler()
            .discoverModels()
            .addImport(modelFile.get().asFile.absolutePath)
            .assemble()
            .result
            .get()

        val services: List<ServiceShape> = model.shapes(ServiceShape::class.java).sorted().toList()
        val service = services.singleOrNull() ?: error("Expected one service per aws model, but found ${services.size} in ${modelFile.get().asFile.absolutePath}: ${services.map { it.id }}")
        val serviceTrait = service.expectTrait(ServiceTrait::class.java)

        val existing = manifest.packages.find { it.sdkId == serviceTrait.sdkId }
        check(existing == null) { "found existing package in manifest for sdkId `${serviceTrait.sdkId}`: $existing" }

        val packages = manifest.packages.toMutableList()
        val newPackage = PackageMetadata.from(serviceTrait.sdkId)
        packages.add(newPackage)
        val updatedManifest = manifest.copy(packages = packages.sortedBy { it.sdkId })

        val contents = json.encodeToString(updatedManifest)
        manifestFile.writeText(contents)
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk

import org.gradle.api.Project
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import java.io.File
import kotlin.streams.toList

/**
 * Represents information needed to generate a smithy projection JSON stanza
 */
data class AwsService(
    /**
     * The service shape ID name
     */
    val serviceShapeId: String,

    /**
     * The package name to use for the service when generating smithy-build.json
     */
    val packageName: String,

    /**
     * The package version (this should match the sdk version of the project)
     */
    val packageVersion: String,

    /**
     * The path to the model file in aws-sdk-kotlin
     */
    val modelFile: File,

    /**
     * The name of the projection to generate
     */
    val projectionName: String,

    /**
     * The sdkId value from the service trait
     */
    val sdkId: String,

    /**
     * The model version from the service shape
     */
    val version: String,

    /**
     * Get the artifact name to use for the service derived from the sdkId. This will be the `A` in the GAV coordinates
     * and the directory name under `services/`.
     */
    val artifactName: String,

    /**
     * A description of the service (taken from the title trait)
     */
    val description: String? = null,
)

/**
 * Returns a lambda for a service model file that respects the given bootstrap config
 *
 * @param project the codegen gradle project
 * @param bootstrap the [BootstrapConfig] used to include/exclude a service based on the given config
 */
fun fileToService(
    project: Project,
    bootstrap: BootstrapConfig,
    pkgManifest: PackageManifest,
): (File) -> AwsService? = { file: File ->
    val sdkVersion = project.findProperty("sdkVersion") as? String ?: error("expected sdkVersion to be set on project ${project.name}")
    val filename = file.nameWithoutExtension
    // TODO - Can't enable validation without being able to recognize all traits which requires additional deps on classpath
    //         This is _OK_ for the build because the CLI will do validation with the correct classpath but for unit tests
    //         it catches some errors that were difficult to track down. Would be nice to enable
    val model = Model.assembler()
        .discoverModels() // FIXME - why needed in tests but not in actual gradle build?
        .addImport(file.absolutePath)
        .assemble()
        .result
        .get()
    val services: List<ServiceShape> = model.shapes(ServiceShape::class.java).sorted().toList()
    val service = services.singleOrNull() ?: error("Expected one service per aws model, but found ${services.size} in ${file.absolutePath}: ${services.joinToString { it.id.toString() }}")
    val protocolName = service.protocolName()

    val serviceTrait = service
        .findTrait(software.amazon.smithy.aws.traits.ServiceTrait.ID)
        .map { it as software.amazon.smithy.aws.traits.ServiceTrait }
        .orNull()
        ?: error("Expected aws.api#service trait attached to model ${file.absolutePath}")

    val sdkId = serviceTrait.sdkId
    val packageName = packageNameForService(sdkId)
    val packageDescription = "The AWS SDK for Kotlin client for $sdkId"

    when {
        !bootstrap.serviceMembership.isMember(filename, packageName) -> {
            project.logger.info("skipping ${file.absolutePath}, $filename/$packageName not a member of ${bootstrap.serviceMembership}")
            null
        }

        !bootstrap.protocolMembership.isMember(protocolName) -> {
            project.logger.info("skipping ${file.absolutePath}, $protocolName not a member of $${bootstrap.protocolMembership}")
            null
        }

        else -> {
            project.logger.info("discovered service: ${serviceTrait.sdkId}")
            // FIXME - re-enable making this an error after migration is finished
            // val pkgMetadata = pkgManifest.bySdkId[sdkId] ?: error("unable to find package metadata for sdkId: $sdkId")
            val pkgMetadata = pkgManifest.bySdkId[sdkId] ?: PackageMetadata.from(sdkId)
            AwsService(
                serviceShapeId = service.id.toString(),
                packageName = pkgMetadata.namespace,
                packageVersion = sdkVersion,
                modelFile = file,
                projectionName = filename,
                sdkId = sdkId,
                version = service.version,
                artifactName = pkgMetadata.artifactName,
                description = packageDescription,
            )
        }
    }
}

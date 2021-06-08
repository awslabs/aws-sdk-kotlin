/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build.plugin

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import java.io.File
import kotlin.streams.toList

fun <T> Project.tryGetClass(className: String): Class<T>? {
    val classLoader = buildscript.classLoader
    return try {
        Class.forName(className, false, classLoader) as Class<T>
    } catch (e: ClassNotFoundException) {
        null
    }
}

fun <T> java.util.Optional<T>.orNull(): T? = this.orElse(null)

fun Logger.aws(message: String) {
    warn("aws: $message")
}

internal val Project.awsModelFile: File
    get() = file("model/service.json")

private const val PLUGIN_KEY_PREFIX = "AwsServicePlugin"
private const val SERVICE_SHAPE_KEY = "$PLUGIN_KEY_PREFIX-ServiceShape"

/**
 * Get the Smithy model instance for this project
 */
internal fun Project.loadModel(): Model {
    // TODO - evaluate how much of a strain this will put on the initial build configure task
    // if it's too much we can fallback to something like id("aws.sdk.kotlin.build.restJson") to provide static mappings
    return Model.assembler().addImport(awsModelFile.absolutePath).assemble().result.get()
}

/**
 * Get the service shape from the smithy model for this project
 */
internal val Project.serviceShape: ServiceShape
    get() {
        val extra = extensions.extraProperties
        // cache this so we don't have to load the model every time it's needed
        if (extra.has(SERVICE_SHAPE_KEY)) {
            return extra.get(SERVICE_SHAPE_KEY) as ServiceShape
        }

        val services: List<ServiceShape> = loadModel().shapes(ServiceShape::class.java).toList()
        require(services.size == 1) { "Expected one service per aws model, but found ${services.size} in ${awsModelFile.absolutePath}" }
        val service = services.first()
        extra.set(SERVICE_SHAPE_KEY, service)
        return service
    }

/**
 * Get the AWS Service trait from the project's model
 */
internal val Project.awsServiceTrait: ServiceTrait
    get() {
        return serviceShape.getTrait(software.amazon.smithy.aws.traits.ServiceTrait::class.java).orNull()
            ?: error { "Expected aws.api#service trait attached to model ${awsModelFile.absolutePath}" }
    }

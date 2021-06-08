/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build.plugin.smithy

import aws.sdk.kotlin.build.plugin.serviceShape
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import software.amazon.smithy.aws.traits.protocols.*
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.Trait

/**
 * Walks a model and configures the runtime dependencies required for the generated service client.
 *
 * This is a custom implementation that replaces the gradle generation of smithy-kotlin.
 */
fun Project.resolveSdkDependencies() {
    // FIXME - for now rely on a simplified static mapping rather than walking the model. In the future, if it's
    //         easy/fast enough we could run codegen in a "dependencies" only mode to spit out the dependencies used
    val protocol = serviceShape.protocolTrait()
    registerHttpDependencies()
    registerAwsDependencies()
    when (protocol) {
        is AwsJson1_0Trait,
        is AwsJson1_1Trait,
        is RestJson1Trait -> registerJsonDependencies()
        is RestXmlTrait -> registerXmlDependencies()
        is AwsQueryTrait -> {
            registerXmlDependencies()
            registerQueryDependencies()
        }
    }
}

private fun ServiceShape.protocolTrait(): Trait {
    val candidate = allTraits.values.firstOrNull{
        when(it.toShapeId()) {
            AwsJson1_0Trait.ID,
            AwsJson1_1Trait.ID,
            RestJson1Trait.ID,
            RestXmlTrait.ID,
            AwsQueryTrait.ID,
            Ec2QueryTrait.ID -> true
            else -> false
        }
    }
    return candidate ?: throw IllegalStateException("unknown protocol ID for service: $id")
}

private val Project.smithyKotlinVersion: String
    get() {
        val version = properties["smithyKotlinVersion"]
        require(version is String) { "smithyKotlinVersion not found in gradle.properties" }
        return version
    }

// FIXME - define configurations to extend instead
private fun Project.registerHttpDependencies() {
    dependencies {
        add("api", "software.aws.smithy.kotlin:client-rt-core:${smithyKotlinVersion}")
        add("implementation", "software.aws.smithy.kotlin:utils:${smithyKotlinVersion}")
        add("implementation", "software.aws.smithy.kotlin:http:${smithyKotlinVersion}")

        add("implementation", "software.aws.smithy.kotlin:http-client-engine-ktor:${smithyKotlinVersion}")
    }
}

private fun Project.registerJsonDependencies() {
    dependencies {
        add("implementation", project(":client-runtime:protocols:aws-json-protocols"))
        add("implementation", "software.aws.smithy.kotlin:serde:${smithyKotlinVersion}")
        add("implementation", "software.aws.smithy.kotlin:serde-json:${smithyKotlinVersion}")
    }
}

private fun Project.registerXmlDependencies() {
    dependencies {
        add("implementation", project(":client-runtime:protocols:aws-xml-protocols"))
        add("implementation", "software.aws.smithy.kotlin:serde:${smithyKotlinVersion}")
        add("implementation", "software.aws.smithy.kotlin:serde-xml:${smithyKotlinVersion}")
    }
}
private fun Project.registerQueryDependencies() {
    dependencies {
        add("implementation", "software.aws.smithy.kotlin:serde:${smithyKotlinVersion}")
        add("implementation", "software.aws.smithy.kotlin:serde-form-url:${smithyKotlinVersion}")
    }
}

private fun Project.registerAwsDependencies() {
    dependencies {
        add("api", project(":client-runtime:auth"))
        add("api", project(":client-runtime:aws-client-rt"))
        add("api", project(":client-runtime:protocols:http"))
        add("api", project(":client-runtime:regions"))
    }
}

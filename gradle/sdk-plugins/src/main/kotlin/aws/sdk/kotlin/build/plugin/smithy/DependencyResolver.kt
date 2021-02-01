/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build.plugin.smithy

import aws.sdk.kotlin.build.plugin.serviceShape
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import software.amazon.smithy.aws.traits.protocols.*

/**
 * Walks a model and configures the runtime dependencies required for the generated service client.
 *
 * This is a custom implementation that replaces the gradle generation of smithy-kotlin.
 */
fun Project.resolveSdkDependencies() {
    // FIXME - for now rely on a simplified static mapping rather than walking the model.
    val protocol = serviceShape.expectTrait(AwsProtocolTrait::class.java)
    registerHttpDependencies()
    registerAwsDependencies()
    when (protocol) {
        is AwsJson1_0Trait,
        is AwsJson1_1Trait,
        is RestJson1Trait -> registerJsonDependencies()
        is RestXmlTrait -> registerXmlDependencies()
    }
}

private object Versions {
    // smithy-kotlin client runtime
    const val ClientRuntime = "0.0.1"
}

private fun Project.registerHttpDependencies() {
    dependencies {
        add("api", "software.aws.smithy.kotlin:client-rt-core:${Versions.ClientRuntime}")
        add("implementation", "software.aws.smithy.kotlin:utils:${Versions.ClientRuntime}")
        add("implementation", "software.aws.smithy.kotlin:http:${Versions.ClientRuntime}")

        add("implementation", "software.aws.smithy.kotlin:http-client-engine-ktor:${Versions.ClientRuntime}")
    }
}

private fun Project.registerJsonDependencies() {
    dependencies {
        add("implementation", project(":client-runtime:protocols:rest-json"))
        add("implementation", "software.aws.smithy.kotlin:serde:${Versions.ClientRuntime}")
        add("implementation", "software.aws.smithy.kotlin:serde-json:${Versions.ClientRuntime}")
    }
}

private fun Project.registerXmlDependencies() {
    dependencies {
        add("implementation", "software.aws.smithy.kotlin:serde:${Versions.ClientRuntime}")
        add("implementation", "software.aws.smithy.kotlin:serde-xml:${Versions.ClientRuntime}")
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

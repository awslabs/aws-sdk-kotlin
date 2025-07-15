/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Extension for configuring the AWS Custom SDK Build plugin.
 * 
 * Provides a type-safe DSL for specifying which AWS services and operations
 * to include in the custom SDK build.
 */
abstract class AwsCustomSdkBuildExtension(internal val project: Project) {
    /**
     * Output directory for generated clients. Defaults to `${project.layout.buildDirectory}/generated-src/aws-sdk`.
     */
    abstract val outputDirectory: Property<String>
    
    /**
     * Package name for generated clients. Defaults to `aws.sdk.kotlin.services`.
     */
    abstract val packageNamePrefix: Property<String>

    /**
     * Set of selected services with their operations
     */
    internal val selectedServices = mutableMapOf<String, Set<String>>()

    init {
        // Set default values
        outputDirectory.convention("${project.layout.buildDirectory}/generated-src/aws-sdk")
        packageNamePrefix.convention("aws.sdk.kotlin.services")
    }

    internal fun service(serviceName: String, operationNames: Set<String>): Provider<Dependency> {
        check(serviceName !in selectedServices) {
            "Service $serviceName configured multiple times. All service configuration should be in a single DSL block."
        }

        selectedServices[serviceName] = operationNames
    }
}

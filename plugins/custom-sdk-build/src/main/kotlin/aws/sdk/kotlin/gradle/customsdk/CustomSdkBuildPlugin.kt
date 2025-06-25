/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin for generating custom AWS SDK clients containing only selected operations.
 * 
 * This plugin allows users to specify which AWS services and operations they need,
 * then generates a custom SDK client with reduced binary size and improved startup times.
 */
class CustomSdkBuildPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Plugin implementation will be added in subsequent prompts
        project.logger.info("Applied AWS SDK Kotlin Custom SDK Build plugin to project ${project.name}")
    }
}

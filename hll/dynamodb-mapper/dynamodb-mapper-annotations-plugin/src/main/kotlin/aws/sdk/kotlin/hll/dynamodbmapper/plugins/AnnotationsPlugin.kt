/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

const val DYNAMODB_MAPPER_EXTENSION_NAME = "dynamoDbMapper"

public class AnnotationsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        check(target == target.rootProject) { "${this::class.java} can only be applied to the root project" }

        t
        println("Annotations plugin applied!")
    }

    private fun Project.installExtension() = extensions.create(DYNAMODB_MAPPER_EXTENSION_NAME, )

    private fun Project.registerCodegenTasks() {

    }
}
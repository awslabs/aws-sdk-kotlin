/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class CodegenTask: DefaultTask() {
    @get:Input
    abstract var projectionName: String

    init {
        group = "codegen"
        description = "Generate code using smithy-kotlin"
    }



    @TaskAction
    fun generateCode() {
        logger.info("generating code for projection: $projectionName")
        // NOTE: this task has dependencies on a smithy build task for the projection
    }

}
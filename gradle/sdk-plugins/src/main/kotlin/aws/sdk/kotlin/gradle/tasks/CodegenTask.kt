/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

abstract class CodegenTask : DefaultTask() {
    init {
        group = "codegen"
        description = "Generate code using smithy-kotlin"
    }

    @TaskAction
    fun generateCode() {
        logger.info("generating kotlin code for projections")
        // NOTE: this task has dependencies on a smithy build task for the projection
        // it doesn't actually do any work (yet) but it does give us our own task to extend _AFTER_ code
        // has been generated
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle.codegen

import aws.sdk.kotlin.gradle.codegen.dsl.SmithyProjection
import aws.sdk.kotlin.gradle.codegen.dsl.projectionRootDir
import org.gradle.api.Project

/**
 * Register and build Smithy projections
 */
open class CodegenExtension(private val project: Project) {

    val projections = project.objects.domainObjectContainer(SmithyProjection::class.java) { name ->
        SmithyProjection(name, project.projectionRootDir(name))
    }
}

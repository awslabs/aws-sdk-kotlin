/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle.codegen

import org.gradle.api.Project

/**
 * Register and build Smithy projections
 */
open class CodegenExtension(private val project: Project) {

    // TODO - allow setting default build settings that apply to every projection (or every projection starts with)?
    val projections = project.objects.domainObjectContainer(KotlinCodegenProjection::class.java) { name ->
        KotlinCodegenProjection(name, project.projectionRootDir(name))
    }
}

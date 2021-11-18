/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle

import org.gradle.api.Action
import org.gradle.api.Project

/**
 * Register and build Smithy projections
 */
open class CodegenExtension(private val project: Project) {
    internal val projections = mutableMapOf<String, KotlinCodegenProjection>()

    // TODO - allow setting default build settings that apply to every projection (or every projection starts with)?

    /**
     * Configure a new projection
     */
    fun projection(name: String, configure: Action<KotlinCodegenProjection>) {
        println("configuring projection $name")
        val p = projections.computeIfAbsent(name) {
            KotlinCodegenProjection(name, project.projectionRootDir(name))
        }
        configure.execute(p)
    }

    /**
     * Execute [action] for each projection
     */
    fun projections(action: Action<in KotlinCodegenProjection>) = projections.values.forEach { action.execute(it) }

    /**
     * Get a projection by name
     */
    fun getProjectionByName(name: String): KotlinCodegenProjection? = projections[name]
}

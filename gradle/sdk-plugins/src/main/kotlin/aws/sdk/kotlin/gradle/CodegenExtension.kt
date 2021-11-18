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


//     FIXME - not all settings make sense to be defaulted...
//    /**
//     * Set default plugin settings that each projection uses if no value is set
//     */
//    private var defaultPluginSettings: SmithyKotlinPluginSettings? = null
//    fun defaultPluginSettings(configure: SmithyKotlinPluginSettings.() -> Unit): Unit {
//        if (defaultPluginSettings == null) { defaultPluginSettings = SmithyKotlinPluginSettings() }
//        defaultPluginSettings!!.apply(configure)
//    }

    /**
     * Configure a new projection
     */
    fun projection(name: String, configure: Action<KotlinCodegenProjection>) {
        println("configuring projection $name")
        val p = KotlinCodegenProjection(name, project.projectionRootDir(name))
        configure.execute(p)
        projections[name] = p
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




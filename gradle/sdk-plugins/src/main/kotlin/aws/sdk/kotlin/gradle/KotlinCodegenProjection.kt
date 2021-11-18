/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle

///**
// * A set of specifications for post-processing the generated files (e.g. remove files, move files around, etc)
// */
//class PostProcessSpec {
//
//
//}

class KotlinCodegenProjection(
    /**
     * The name of the projection
     */
    val name: String,

    /**
     * Root directory for this projection
     */
    val projectionRootDir: java.io.File,
){

    /**
     * List of files/directories to import when building the projection
     *
     * See https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#projections
     */
    var imports: List<String> = emptyList()


    internal var pluginSettings: SmithyKotlinPluginSettings = SmithyKotlinPluginSettings()

    /**
     * Configure smithy-kotlin plugin settings.
     */
    fun pluginSettings(configure: SmithyKotlinPluginSettings.() -> Unit): Unit { pluginSettings.also(configure) }


    //    private var postProcessSpec: PostProcessSpec? = null
    //    fun postProcess(spec: PostProcessSpec.() -> Unit) {
    //        postProcessSpec = PostProcessSpec().apply(spec)
    //    }
}


class SmithyKotlinPluginSettings {
    var serviceShapeId: String? = null
    var packageName: String? = null
    var packageVersion: String? = null
    var packageDescription: String? = null
    var sdkId: String? = null
    var buildSettings: Map<String, Any>? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SmithyKotlinPluginSettings

        if (serviceShapeId != other.serviceShapeId) return false
        if (packageName != other.packageName) return false
        if (packageVersion != other.packageVersion) return false
        if (packageDescription != other.packageDescription) return false
        if (sdkId != other.sdkId) return false
        if (buildSettings != other.buildSettings) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serviceShapeId?.hashCode() ?: 0
        result = 31 * result + (packageName?.hashCode() ?: 0)
        result = 31 * result + (packageVersion?.hashCode() ?: 0)
        result = 31 * result + (packageDescription?.hashCode() ?: 0)
        result = 31 * result + (sdkId?.hashCode() ?: 0)
        result = 31 * result + (buildSettings?.hashCode() ?: 0)
        return result
    }
}
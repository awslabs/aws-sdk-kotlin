/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle.codegen

import software.amazon.smithy.model.node.ArrayNode
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.node.ToNode
import java.util.*

// /**
// * A set of specifications for post-processing the generated files (e.g. remove files, move files around, etc)
// */
// class PostProcessSpec {
//
//
// }

class KotlinCodegenProjection(
    /**
     * The name of the projection
     */
    val name: String,

    /**
     * Root directory for this projection
     */
    val projectionRootDir: java.io.File,
) {

    /**
     * List of files/directories to import when building the projection
     *
     * See https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#projections
     */
    var imports: List<String> = emptyList()

    /**
     * A list of transforms to apply
     *
     * See https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#transforms
     */
    var transforms: List<String> = emptyList()

    internal var pluginSettings: SmithyKotlinPluginSettings = SmithyKotlinPluginSettings()

    /**
     * Configure smithy-kotlin plugin settings.
     */
    fun pluginSettings(configure: SmithyKotlinPluginSettings.() -> Unit) { pluginSettings.also(configure) }

    //    private var postProcessSpec: PostProcessSpec? = null
    //    fun postProcess(spec: PostProcessSpec.() -> Unit) {
    //        postProcessSpec = PostProcessSpec().apply(spec)
    //    }

    internal fun toNode(): Node {
        // escape windows paths for valid json
        val formattedImports = imports
            .map { it.replace("\\", "\\\\") }

        val transformNodes = transforms.map { Node.parse(it) }
        val obj = ObjectNode.objectNodeBuilder()
            .withArrayMember("imports", formattedImports)
            .withMember("transforms", ArrayNode.fromNodes(transformNodes))
            .withObjectMember("plugins") {
                withMember("kotlin-codegen", pluginSettings.toNode())
            }
        return obj.build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotlinCodegenProjection

        if (name != other.name) return false
        if (imports != other.imports) return false
        if (transforms != other.transforms) return false
        if (pluginSettings != other.pluginSettings) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + imports.hashCode()
        result = 31 * result + transforms.hashCode()
        result = 31 * result + pluginSettings.hashCode()
        return result
    }
}

class SmithyKotlinBuildSettings : ToNode {
    var generateFullProject: Boolean? = null
    var generateDefaultBuildFiles: Boolean? = null
    var optInAnnotations: List<String>? = null

    override fun toNode(): Node {
        val builder = ObjectNode.objectNodeBuilder()

        builder.withOptionalMember("rootProject", generateFullProject)
        builder.withOptionalMember("generateDefaultBuildFiles", generateDefaultBuildFiles)

        val optInArrNode = optInAnnotations?.map { Node.from(it) }?.let { ArrayNode.fromNodes(it) }
        builder.withOptionalMember("optInAnnotations", Optional.ofNullable(optInArrNode))
        return builder.build()
    }
}

class SmithyKotlinPluginSettings {
    var serviceShapeId: String? = null
    var packageName: String? = null
    var packageVersion: String? = null
    var packageDescription: String? = null
    var sdkId: String? = null

    internal var buildSettings: SmithyKotlinBuildSettings? = null
    fun buildSettings(configure: SmithyKotlinBuildSettings.() -> Unit) {
        if (buildSettings == null) buildSettings = SmithyKotlinBuildSettings()
        buildSettings!!.apply(configure)
    }

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

    internal fun toNode(): Node {
        val obj = ObjectNode.objectNodeBuilder()
            .withMember("service", serviceShapeId!!)
            .withObjectMember("package") {
                withMember("name", packageName!!)
                withOptionalMember("version", packageVersion)
                withOptionalMember("version", packageDescription)
            }
            .withOptionalMember("sdkId", sdkId)
            .withOptionalMember("build", buildSettings)

        return obj.build()
    }
}

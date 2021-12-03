/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle.codegen.dsl

import software.amazon.smithy.model.node.ArrayNode
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.node.ToNode
import java.util.*

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

class SmithyKotlinPluginSettings : SmithyBuildPlugin {
    override val pluginName: String = "kotlin-codegen"

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

    override fun toNode(): Node {
        val obj = ObjectNode.objectNodeBuilder()
            .withMember("service", serviceShapeId!!)
            .withObjectMember("package") {
                withMember("name", packageName!!)
                withOptionalMember("version", packageVersion)
                withOptionalMember("description", packageDescription)
            }
            .withOptionalMember("sdkId", sdkId)
            .withOptionalMember("build", buildSettings)

        return obj.build()
    }
}

fun SmithyProjection.smithyKotlinPlugin(configure: SmithyKotlinPluginSettings.() -> Unit) {
    val p = plugins.computeIfAbsent("kotlin-codegen") { SmithyKotlinPluginSettings() } as SmithyKotlinPluginSettings
    p.apply(configure)
}

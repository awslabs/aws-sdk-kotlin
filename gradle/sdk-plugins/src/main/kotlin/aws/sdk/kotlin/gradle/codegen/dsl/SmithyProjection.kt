/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle.codegen.dsl

import software.amazon.smithy.model.node.ArrayNode
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode

/**
 * A container for settings related to a single Smithy projection.
 *
 * See https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#projections
 */
class SmithyProjection(
    /**
     * The name of the projection
     */
    val name: String,

    // FIXME - technically this is based on plugin. Should the projection root dir be based on plugin as well rather than a single field?
    /**
     * Root directory for this projection
     */
    val projectionRootDir: java.io.File,
) {

    /**
     * List of files/directories to import when building the projection
     */
    var imports: List<String> = emptyList()

    /**
     * A list of transforms to apply
     *
     * See https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#transforms
     */
    var transforms: List<String> = emptyList()

    /**
     * Plugin name to plugin settings. Plugins should provide an extension function to configure their own plugin settings
     */
    val plugins: MutableMap<String, SmithyBuildPlugin> = mutableMapOf()

    internal fun toNode(): Node {
        // escape windows paths for valid json
        val formattedImports = imports
            .map { it.replace("\\", "\\\\") }

        val transformNodes = transforms.map { Node.parse(it) }
        val obj = ObjectNode.objectNodeBuilder()
            .withArrayMember("imports", formattedImports)
            .withMember("transforms", ArrayNode.fromNodes(transformNodes))

        if (plugins.isNotEmpty()) {
            obj.withObjectMember("plugins") {
                plugins.forEach { (pluginName, pluginSettings) ->
                    withMember(pluginName, pluginSettings.toNode())
                }
            }
        }
        return obj.build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SmithyProjection

        if (name != other.name) return false
        if (projectionRootDir != other.projectionRootDir) return false
        if (imports != other.imports) return false
        if (transforms != other.transforms) return false
        if (plugins != other.plugins) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + projectionRootDir.hashCode()
        result = 31 * result + imports.hashCode()
        result = 31 * result + transforms.hashCode()
        result = 31 * result + plugins.hashCode()
        return result
    }
}

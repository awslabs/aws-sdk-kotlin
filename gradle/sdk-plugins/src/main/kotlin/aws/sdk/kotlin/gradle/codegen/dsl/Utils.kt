/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.gradle.codegen.dsl

import aws.sdk.kotlin.gradle.codegen.CODEGEN_EXTENSION_NAME
import aws.sdk.kotlin.gradle.codegen.CodegenExtension
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.get
import software.amazon.smithy.model.node.ArrayNode
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.node.ToNode
import java.util.*

/**
 * Get the root directory of the generated kotlin code for a projection
 */
fun Project.projectionRootDir(projectionName: String): java.io.File =
    file("${project.buildDir}/smithyprojections/${project.name}/$projectionName/kotlin-codegen")

/**
 * Get the [CodegenExtension] instance configured for the project
 */
internal val Project.codegenExtension: CodegenExtension
    get() = ((this as ExtensionAware).extensions[CODEGEN_EXTENSION_NAME] as? CodegenExtension) ?: error("CodegenPlugin has not been applied")

internal fun ObjectNode.Builder.withObjectMember(key: String, block: ObjectNode.Builder.() -> Unit): ObjectNode.Builder {
    val builder = ObjectNode.objectNodeBuilder()
    builder.apply(block)
    return withMember(key, builder.build())
}
internal fun ObjectNode.Builder.withNullableMember(key: String, member: String?): ObjectNode.Builder = apply {
    if (member == null) return this
    return withMember(key, member)
}

internal fun ObjectNode.Builder.withNullableMember(key: String, member: Boolean?): ObjectNode.Builder = apply {
    if (member == null) return this
    return withMember(key, member)
}

internal fun <T : ToNode> ObjectNode.Builder.withNullableMember(key: String, member: T?): ObjectNode.Builder =
    withOptionalMember(key, Optional.ofNullable(member))

internal fun ObjectNode.Builder.withArrayMember(key: String, member: List<String>): ObjectNode.Builder = apply {
    val arrNode = member.map { Node.from(it) }.let { ArrayNode.fromNodes(it) }
    return withMember(key, arrNode)
}

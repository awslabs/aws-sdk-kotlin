/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.protocols.core

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.model.node.ObjectNode

class ExtraMetadataGenerator(private val writer: KotlinWriter, private val metadataNode: ObjectNode) {
    companion object {
        fun getSymbol(settings: KotlinSettings): Symbol = buildSymbol {
            name = "extraMetadata"
            namespace = "${settings.pkg.name}.metadata"
        }
    }

    fun render() {
        writer.withBlock("internal val extraMetadata: Map<String, String> = mapOf(", ")") {
            metadataNode
                .stringMap
                .mapValues { it.value.expectStringNode().value }
                .forEach { (key, value) -> writer.write("#S to #S,", key, value) }
        }
    }
}

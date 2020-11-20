/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package com.amazonaws.service.s3.model

import software.aws.clientrt.serde.*

class Tag private constructor(builder: BuilderImpl) {

    val key: String? = builder.key
    val value: String? = builder.value

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
        fun builder(): DslBuilder = BuilderImpl()

        private val KEY_FIELD_DESCRIPTOR = SdkFieldDescriptor("Key", SerialKind.String)
        private val VALUE_FIELD_DESCRIPTOR = SdkFieldDescriptor("Value", SerialKind.String)

        private val OBJ_DESCRIPTOR = SdkObjectDescriptor.build() {
            serialName = "Tag"
            field(KEY_FIELD_DESCRIPTOR)
            field(VALUE_FIELD_DESCRIPTOR)
        }

        fun deserialize(deserializer: Deserializer): Tag {
            val builder = builder()

            deserializer.deserializeStruct(OBJ_DESCRIPTOR) {
                loop@ while (true) {
                    when (findNextFieldIndex()) {
                        KEY_FIELD_DESCRIPTOR.index -> builder.key = deserializeString()
                        VALUE_FIELD_DESCRIPTOR.index -> builder.value = deserializeString()
                        null -> break@loop
                        else -> skipValue()
                    }
                }
            }
            return builder.build()
        }
    }

    interface Builder {
        fun build(): Tag
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        var key: String?
        var value: String?

        fun build(): Tag
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var key: String? = null
        override var value: String? = null

        override fun build(): Tag = Tag(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tag

        if (key != other.key) return false
        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key?.hashCode() ?: 0
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Tag(key=$key, value=$value)"
    }
}
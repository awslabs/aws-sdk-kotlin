/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package com.amazonaws.service.s3.model

class GetBucketTaggingResponse  private constructor(builder: BuilderImpl) {
    val tagSet: List<Tag>? = builder.tagSet

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
        fun builder(): DslBuilder = BuilderImpl()
    }

    interface Builder {
        fun build(): GetBucketTaggingResponse
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        var tagSet: List<Tag>?
        fun build(): GetBucketTaggingResponse
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var tagSet: List<Tag>? = null


        override fun build(): GetBucketTaggingResponse = GetBucketTaggingResponse(this)
    }

    override fun toString(): String {
        return "GetBucketTaggingResponse(tagSet=$tagSet)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GetBucketTaggingResponse

        if (tagSet != other.tagSet) return false

        return true
    }

    override fun hashCode(): Int {
        return tagSet?.hashCode() ?: 0
    }
}
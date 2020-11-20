/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package com.amazonaws.service.s3.model

class GetBucketTaggingRequest private constructor(builder: BuilderImpl) {
    val bucket: String? = builder.bucket

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
    }

    interface Builder {
        fun build(): GetBucketTaggingRequest
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        // Location: URI
        var bucket: String?
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var bucket: String? = null

        override fun build(): GetBucketTaggingRequest = GetBucketTaggingRequest(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GetBucketTaggingRequest

        if (bucket != other.bucket) return false

        return true
    }

    override fun hashCode(): Int {
        return bucket?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "GetBucketTaggingRequest(bucket=$bucket)"
    }
}

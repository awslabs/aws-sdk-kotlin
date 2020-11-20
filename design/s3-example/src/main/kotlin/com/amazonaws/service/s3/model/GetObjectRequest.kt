/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.service.s3.model


class GetObjectRequest private constructor(builder: BuilderImpl){

    val bucket: String? = builder.bucket
    val key: String? = builder.key
    val ifMatch: String? = builder.ifMatch
    val ifModifiedSince: String? = builder.ifModifiedSince
    val ifNoneMatch: String? = builder.ifNoneMatch
    val ifUnmodifiedSince: String? = builder.ifUnmodifiedSince
    val partNumber: Long? = builder.partNumber
    val range: String? = builder.range
    val requestPayer: String? = builder.requestPayer
    val responseCacheControl: String? = builder.responseCacheControl
    val responseContentDisposition: String? = builder.responseContentDisposition
    val responseContentEncoding: String? = builder.responseContentEncoding
    val responseContentLanguage: String? = builder.responseContentLanguage
    val responseContentType: String? = builder.responseContentType
    val responseExpires: String? = builder.responseExpires
    val sseCustomerAlgorithm: String? = builder.sseCustomerAlgorithm
    val sseCustomerKey: String? = builder.sseCustomerKey
    val sseCustomerKeyMd5: String? = builder.sseCustomerKeyMd5
    val versionId: String? = builder.versionId

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
    }

    interface Builder {
        fun build(): GetObjectRequest
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        // Location: URI; Name: "bucket"
        var bucket: String?
        // Location: URI; Name: "key"
        var key: String?
        // Location: HEADER; Name: "If-Match"
        var ifMatch: String?
        // Location: HEADER; Name: "If-Modified-Since"
        var ifModifiedSince: String?
        // Location: HEADER; Name: "If-None-Match"
        var ifNoneMatch: String?
        // Location: HEADER; Name: "If-Unmodified-Since"
        var ifUnmodifiedSince: String?
        var partNumber: Long?
        var range: String?
        var requestPayer: String?
        var responseCacheControl: String?
        var responseContentDisposition: String?
        var responseContentEncoding: String?
        var responseContentLanguage: String?
        var responseContentType: String?
        var responseExpires: String?
        var sseCustomerAlgorithm: String?
        var sseCustomerKey: String?
        var sseCustomerKeyMd5: String?
        var versionId: String?

    }

    private class BuilderImpl : Builder, DslBuilder {
        override var bucket: String? = null
        override var key: String? = null
        override var ifMatch: String? = null
        override var ifModifiedSince: String? = null
        override var ifNoneMatch: String? = null
        override var ifUnmodifiedSince: String? = null
        override var partNumber: Long? = null
        override var range: String? = null
        override var requestPayer: String? = null
        override var responseCacheControl: String? = null
        override var responseContentDisposition: String? = null
        override var responseContentEncoding: String? = null
        override var responseContentLanguage: String? = null
        override var responseContentType: String? = null
        override var responseExpires: String? = null
        override var sseCustomerAlgorithm: String? = null
        override var sseCustomerKey: String? = null
        override var sseCustomerKeyMd5: String? = null
        override var versionId: String? = null
        override fun build(): GetObjectRequest = GetObjectRequest(this)
    }
}

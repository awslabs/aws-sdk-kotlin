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

import software.aws.clientrt.content.ByteStream

class GetObjectResponse private constructor(builder: BuilderImpl){

    val body: ByteStream? = builder.body
    val acceptRanges: String? = builder.acceptRanges
    val cacheControl: String? = builder.cacheControl
    val contentDisposition: String? = builder.contentDisposition
    val contentEncoding: String? = builder.contentEncoding
    val contentLanguage: String? = builder.contentLanguage
    val contentLength: Long? = builder.contentLength
    val contentRange: String? = builder.contentRange
    val contentType: String? = builder.contentType
    val deleteMarker: Boolean? = builder.deleteMarker
    val eTag: String? = builder.eTag
    val expiration: String? = builder.expiration
    val expires: String? = builder.expires
    val lastModified: String? = builder.lastModified
    val metadata: Map<String, String>? = builder.metadata
    val missingMeta: Long? = builder.missingMeta
    val objectLockLegalHoldStatus: String? = builder.objectLockLegalHoldStatus
    val objectLockMode: String? = builder.objectLockMode
    val objectLockRetainUntilDate: String? = builder.objectLockRetainUntilDate
    val partsCount: Long? = builder.partsCount
    val replicationStatus: String? = builder.replicationStatus
    val requestCharged: String? = builder.requestCharged
    val restore: String? = builder.restore
    val sseCustomerAlgorithm: String? = builder.sseCustomerAlgorithm
    val sseCustomerKeyMd5: String? = builder.sseCustomerKeyMd5
    val ssekmsKeyId: String? = builder.ssekmsKeyId
    val serverSideEncryption: String? = builder.serverSideEncryption
    val storageClass: String? = builder.storageClass
    val tagCount: Long? = builder.tagCount
    val versionId: String? = builder.versionId
    val websiteRedirectLocation: String? = builder.websiteRedirectLocation


    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
    }

    interface Builder {
        fun build(): GetObjectResponse
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        // Payload
        var body: ByteStream?

        // Header Values
        var acceptRanges: String?
        var cacheControl: String?
        var contentDisposition: String?
        var contentEncoding: String?
        var contentLanguage: String?
        var contentLength: Long?
        var contentRange: String?
        var contentType: String?
        var deleteMarker: Boolean?
        var eTag: String?
        var expiration: String?
        var expires: String?
        var lastModified: String?
        var metadata: Map<String, String>?
        var missingMeta: Long?
        var objectLockLegalHoldStatus: String?
        var objectLockMode: String?
        var objectLockRetainUntilDate: String?
        var partsCount: Long?
        var replicationStatus: String?
        var requestCharged: String?
        var restore: String?
        var sseCustomerAlgorithm: String?
        var sseCustomerKeyMd5: String?
        var ssekmsKeyId: String?
        var serverSideEncryption: String?
        var storageClass: String?
        var tagCount: Long?
        var versionId: String?
        var websiteRedirectLocation: String?

    }

    private class BuilderImpl : Builder, DslBuilder {
        override var body: ByteStream? = null
        override var acceptRanges: String? = null
        override var cacheControl: String? = null
        override var contentDisposition: String? = null
        override var contentEncoding: String? = null
        override var contentLanguage: String? = null
        override var contentLength: Long? = null
        override var contentRange: String? = null
        override var contentType: String? = null
        override var deleteMarker: Boolean? = null
        override var eTag: String? = null
        override var expiration: String? = null
        override var expires: String? = null
        override var lastModified: String? = null
        override var metadata: Map<String, String>? = null
        override var missingMeta: Long? = null
        override var objectLockLegalHoldStatus: String? = null
        override var objectLockMode: String? = null
        override var objectLockRetainUntilDate: String? = null
        override var partsCount: Long? = null
        override var replicationStatus: String? = null
        override var requestCharged: String? = null
        override var restore: String? = null
        override var sseCustomerAlgorithm: String? = null
        override var sseCustomerKeyMd5: String? = null
        override var ssekmsKeyId: String? = null
        override var serverSideEncryption: String? = null
        override var storageClass: String? = null
        override var tagCount: Long? = null
        override var versionId: String? = null
        override var websiteRedirectLocation: String? = null

        override fun build(): GetObjectResponse = GetObjectResponse(this)
    }
}

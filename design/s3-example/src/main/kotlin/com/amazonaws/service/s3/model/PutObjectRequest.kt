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

class PutObjectRequest private constructor(builder: BuilderImpl){

    val cacheControl: String? = builder.cacheControl
    val contentDisposition: String? = builder.contentDisposition
    val contentType: String? = builder.contentType
    val contentLength: Int? = builder.contentLength

    // ... 20 more headers later

    //
    val body: ByteStream? = builder.body
    val bucket: String? = builder.bucket
    val key: String? = builder.key


    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
    }

    interface Builder {
        fun build(): PutObjectRequest
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        // Location: HEADER; Name: "Cache-Control"
        var cacheControl: String?

        // Location: HEADER; Name: "Content-Disposition"
        var contentDisposition: String?

        // Location: HEADER; Name: "Content-Type"
        var contentType: String?
        
        // Location: HEADER; Name: "Content-Length"
        var contentLength: Int?

        // Location: PAYLOAD
        var body: ByteStream?
        
        // Location: URI
        var bucket: String?
        
        // Location: URI
        var key: String?
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var cacheControl: String? = null
        override var contentDisposition: String? = null
        override var contentType: String? = null
        override var contentLength: Int? = null
        override var body: ByteStream? = null
        override var bucket: String? = null
        override var key: String? = null
        
        override fun build(): PutObjectRequest = PutObjectRequest(this)
    }
}

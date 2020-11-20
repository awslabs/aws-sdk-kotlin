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


class PutObjectResponse private constructor(builder: BuilderImpl){
    val eTag: String? = builder.eTag
    val expiration: String? = builder.expiration
    val requestCharged: String? = builder.requestCharged
    val sseCustomerAlgorithm: String? = builder.sseCustomerAlgorithm
    val sseCustomerKeyMd5: String? = builder.sseCustomerKeyMd5
    val sseKmsEncryptionContext: String? = builder.sseKmsEncryptionContext
    val sseKmsKeyId: String? = builder.sseKmsKeyId
    val serverSideEncryption: String? = builder.serverSideEncryption
    val versionId: String? = builder.versionId

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
    }

    override fun toString(): String = buildString {
        // real toString will be different this is for demo purposes
        appendln("PutObjectResponse {")
        appendln("\tETag: $eTag")
        appendln("\tExpiration: $expiration")
        appendln("\tRequestCharged: $requestCharged")
        appendln("\tSseCustomerAlgorithm: $sseCustomerAlgorithm")
        appendln("\tSseCustomerKeyMd5: $sseCustomerKeyMd5")
        appendln("\tSseKmsEncryptionContext: $sseKmsEncryptionContext")
        appendln("\tSseKmsKeyId: $sseKmsKeyId")
        appendln("\tServerSideEncryption: $serverSideEncryption")
        appendln("\tVersionId: $versionId")
        appendln("}")
    }

    interface Builder {
        fun build(): PutObjectResponse
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        // Location: HEADER; Name: "ETag"
        var eTag: String?
        // Location: HEADER; Name: "x-amz-expiration"
        var expiration: String?
        // Location: HEADER; Name: "x-amz-request-charged"
        var requestCharged: String?
        // Location: HEADER; Name: "x-amz-server-side-encryption-customer-algorithm"
        var sseCustomerAlgorithm: String?
        // Location: HEADER; Name: "x-amz-server-side-encryption-customer-key-MD5"
        var sseCustomerKeyMd5: String?
        // Location: HEADER; Name: "x-amz-server-side-encryption-context"
        var sseKmsEncryptionContext: String?
        // Location: HEADER; Name: "x-amz-server-side-encryption-aws-kms-key-id"
        var sseKmsKeyId: String?
        // Location: HEADER; Name: "x-amz-server-side-encryption"
        var serverSideEncryption: String?
        // Location: HEADER; Name: "x-amz-version-id"
        var versionId: String?
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var eTag: String? = null
        override var expiration: String? = null
        override var requestCharged: String? = null
        override var sseCustomerAlgorithm: String? = null
        override var sseCustomerKeyMd5: String? = null
        override var sseKmsEncryptionContext: String? = null
        override var sseKmsKeyId: String? = null
        override var serverSideEncryption: String? = null
        override var versionId: String? = null


        override fun build(): PutObjectResponse = PutObjectResponse(this)
    }
}

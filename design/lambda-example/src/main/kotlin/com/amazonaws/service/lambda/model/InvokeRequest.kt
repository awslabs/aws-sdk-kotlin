/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package com.amazonaws.service.lambda.model

class InvokeRequest private constructor(builder: BuilderImpl){

    val functionName: String? = builder.functionName
    val invocationType: String? = builder.invocationType
    val logType: String? = builder.logType
    val clientContext: String? = builder.clientContext
    val payload: ByteArray? = builder.payload
    val qualifier: String? = builder.qualifier
    val idempotencyToken: String? = builder.idempotencyToken

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
    }

    interface Builder {
        fun build(): InvokeRequest
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        // Location: PATH; Name: "FunctionName"
        var functionName: String?

        // Location: HEADER; Name: "X-Amz-Invocation-Type"
        var invocationType: String?

        // Location: HEADER; Name: "X-Amz-Log-Type"
        var logType: String?

        // Location: HEADER; Name: "X-Amz-Client-Context"
        var clientContext: String?

        // Location: PAYLOAD; Name: "Payload"
        var payload: ByteArray?

        // Location: QUERY; Name: "Qualifier"
        var qualifier: String?

        // Location: Header; Name: "idempotencyToken"
        var idempotencyToken: String?
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var functionName: String? = null
        override var invocationType: String? = null
        override var logType: String? = null
        override var clientContext: String? = null
        override var payload: ByteArray? = null
        override var qualifier: String? = null
        override var idempotencyToken: String? = null
        override fun build(): InvokeRequest = InvokeRequest(this)
    }
}


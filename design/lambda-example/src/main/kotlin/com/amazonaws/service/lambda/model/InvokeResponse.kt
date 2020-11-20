/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package com.amazonaws.service.lambda.model


class InvokeResponse private constructor(builder: BuilderImpl){

    val statusCode: Int? = builder.statusCode
    val functionError: String? = builder.functionError
    val logResult: String? = builder.logResult
    val payload: ByteArray? = builder.payload
    val executedVersion: String? = builder.executedVersion

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = buildString {
        append("InvokeResponse{\n")
        append("\tStatusCode: $statusCode\n")
        append("\tLogResult: $logResult\n")
        append("\tExecutedVersion: $executedVersion\n")
        append("\tPayload: ${payload?.decodeToString()}")
        append("\n}")
    }

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
    }

    interface Builder {
        fun build(): InvokeResponse
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        var statusCode: Int?
        var functionError: String?
        var logResult: String?
        var payload: ByteArray?
        var executedVersion: String?
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var statusCode: Int? = null
        override var functionError: String? = null
        override var logResult: String? = null
        override var payload: ByteArray? = null
        override var executedVersion: String? = null
        override fun build(): InvokeResponse = InvokeResponse(this)
    }
}
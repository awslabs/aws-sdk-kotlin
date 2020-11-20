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
package com.amazonaws.service.lambda

import com.amazonaws.service.lambda.model.AliasConfiguration
import com.amazonaws.service.lambda.model.CreateAliasRequest
import com.amazonaws.service.lambda.model.InvokeRequest
import com.amazonaws.service.lambda.model.InvokeResponse
import software.aws.clientrt.SdkClient
import software.aws.clientrt.config.IdempotencyTokenConfig
import software.aws.clientrt.config.IdempotencyTokenProvider
import software.aws.clientrt.http.config.HttpClientConfig
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.engine.HttpClientEngineConfig


interface LambdaClient: SdkClient {
    override val serviceName: String
        get() = "lambda"

    companion object {
        operator fun invoke(block: Config.DslBuilder.() -> Unit = {}): LambdaClient {
            val config = Config.BuilderImpl().apply(block).build()
            return DefaultLambdaClient(config)
        }
    }

    class Config private constructor(builder: BuilderImpl): HttpClientConfig, IdempotencyTokenConfig {
        override val httpClientEngine: HttpClientEngine? = builder.httpClientEngine
        override val httpClientEngineConfig: HttpClientEngineConfig? = builder.httpClientEngineConfig
        override val idempotencyTokenProvider: IdempotencyTokenProvider? = builder.idempotencyTokenProvider

        companion object {
            @JvmStatic
            fun builder(): Builder = BuilderImpl()

            fun dslBuilder(): DslBuilder = BuilderImpl()

            operator fun invoke(block: DslBuilder.() -> Unit): Config = BuilderImpl().apply(block).build()
        }

        fun copy(block: DslBuilder.() -> Unit = {}): Config = BuilderImpl(this).apply(block).build()

        interface Builder {
            fun build(): Config
            fun httpClientEngine(httpClientEngine: HttpClientEngine): Builder
            fun httpClientEngineConfig(httpClientEngineConfig: HttpClientEngineConfig): Builder
            fun idempotencyTokenProvider(idempotencyTokenProvider: IdempotencyTokenProvider): Builder
        }

        interface DslBuilder {
            var httpClientEngine: HttpClientEngine?
            var httpClientEngineConfig: HttpClientEngineConfig?
            var idempotencyTokenProvider: IdempotencyTokenProvider?

            fun build(): Config
        }

        internal class BuilderImpl() : Builder, DslBuilder {
            override var httpClientEngine: HttpClientEngine? = null
            override var httpClientEngineConfig: HttpClientEngineConfig? = null
            override var idempotencyTokenProvider: IdempotencyTokenProvider? = null

            constructor(x: Config) : this() {
                this.httpClientEngine = x.httpClientEngine
                this.httpClientEngineConfig = x.httpClientEngineConfig
                this.idempotencyTokenProvider = x.idempotencyTokenProvider
            }

            override fun build(): Config = Config(this)
            override fun httpClientEngine(httpClientEngine: HttpClientEngine): Builder = apply { this.httpClientEngine = httpClientEngine }
            override fun httpClientEngineConfig(httpClientEngineConfig: HttpClientEngineConfig): Builder = apply { this.httpClientEngineConfig = httpClientEngineConfig }
            override fun idempotencyTokenProvider(idempotencyTokenProvider: IdempotencyTokenProvider): Builder = apply { this.idempotencyTokenProvider = idempotencyTokenProvider }
        }
    }

    suspend fun invoke(input: InvokeRequest): InvokeResponse
    suspend fun invoke(block: InvokeRequest.DslBuilder.() -> Unit): InvokeResponse {
        val input = InvokeRequest{ block(this) }
        return invoke(input)
    }

    suspend fun createAlias(input: CreateAliasRequest): AliasConfiguration

    suspend fun createAlias(block: CreateAliasRequest.DslBuilder.() -> Unit): AliasConfiguration {
        val input = CreateAliasRequest{ block(this) }
        return createAlias(input)
    }
}
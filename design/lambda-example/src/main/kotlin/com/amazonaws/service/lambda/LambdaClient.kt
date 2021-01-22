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

import aws.sdk.kotlin.runtime.InternalSdkApi
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
import aws.sdk.kotlin.runtime.auth.AuthConfig
import aws.sdk.kotlin.runtime.auth.CredentialsProvider
import aws.sdk.kotlin.runtime.regions.RegionConfig


interface LambdaClient: SdkClient {
    override val serviceName: String
        get() = "lambda"

    companion object {
        @InternalSdkApi
        operator fun invoke(block: Config.DslBuilder.() -> Unit = {}): LambdaClient {
            val config = Config.BuilderImpl().apply(block).build()
            return DefaultLambdaClient(config)
        }
    }

    // generated per/client
    class Config private constructor(builder: BuilderImpl): AuthConfig, RegionConfig, HttpClientConfig, IdempotencyTokenConfig {
        override val region: String? = builder.region
        override val signingRegion: String? = builder.signingRegion
        override val credentialsProvider: CredentialsProvider? = builder.credentialsProvider
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
            // TODO - missing region, cred provider, etc
        }

        interface DslBuilder {
            /**
             * The AWS region the client should use. Note this is not always the same as [signingRegion] in
             * the case of global services like IAM
             */
            var region: String?

            /**
             * AWS region to be used for signing the request
             */
            var signingRegion: String?

            /**
             * The [CredentialsProvider] the client should use to sign requests with. If not specified a default
             * provider will be used.
             */
            var credentialsProvider: CredentialsProvider?

            /**
             * Override the HTTP client engine used to make requests with. This is a more advanced option that allows
             * you to BYO client engine or share engines across service clients. Caller is responsible for any cleanup
             * associated with the engine and ensuring it's resources are disposed of properly.
             */
            var httpClientEngine: HttpClientEngine?

            /**
             * Override the default HTTP client engine config
             */
            var httpClientEngineConfig: HttpClientEngineConfig?

            var idempotencyTokenProvider: IdempotencyTokenProvider?

            fun build(): Config
        }

        internal class BuilderImpl() : Builder, DslBuilder {
            // TODO - we could inherit from ClientOptionsBuilder and delegate some of these into a default PropertyBag
            override var region: String? = null
            override var signingRegion: String? = null
            override var credentialsProvider: CredentialsProvider? = null
            override var httpClientEngine: HttpClientEngine? = null
            override var httpClientEngineConfig: HttpClientEngineConfig? = null
            override var idempotencyTokenProvider: IdempotencyTokenProvider? = null

            constructor(x: Config) : this() {
                this.region = x.region
                this.signingRegion = x.signingRegion
                this.credentialsProvider = x.credentialsProvider
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
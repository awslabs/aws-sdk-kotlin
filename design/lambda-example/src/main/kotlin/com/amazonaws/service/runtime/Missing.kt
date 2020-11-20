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

package com.amazonaws.service.runtime

import software.aws.clientrt.http.ExecutionContext
import software.aws.clientrt.http.SdkHttpClient
import software.aws.clientrt.http.config.HttpClientConfig
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.engine.HttpClientEngineConfig
import software.aws.clientrt.util.AttributeKey
import software.aws.clientrt.util.Attributes
import software.aws.kotlinsdk.ClientException
import software.aws.kotlinsdk.auth.AuthConfig
import software.aws.kotlinsdk.auth.CredentialsProvider
import software.aws.kotlinsdk.regions.RegionConfig

// ######################################################################################
// Things either missing or in-progress in the client runtime.
// ######################################################################################


/**
 * A (service) client option that influences how the client behaves when executing requests
 */
typealias ClientOption<T> = AttributeKey<T>

/**
 * A collection of AWS service client options. NOTE: most options are configured by default through the service
 * config.
 */
object AwsClientOption {
    /**
     * The AWS region the client should use. Note this is not always the same as [SigningRegion] in
     * the case of global services like IAM
     */
    val Region: ClientOption<String> = ClientOption("AwsRegion")

    /**
     * AWS region to be used for signing the request
     */
    val SigningRegion: ClientOption<String> = ClientOption("AwsSigningRegion")

    /**
     * Scope name to use during signing of a request
     */
    val ServiceSigningName: ClientOption<String> = ClientOption("AwsServiceSigningName")

    /**
     * The first part of the URL in the DNS name for the service. Eg. in the endpoint "dynamodb.amazonaws.com"
     * this is the "dynamodb" part
     */
    val EndpointPrefix: ClientOption<String> = ClientOption("EndpointPrefix")


    // FIXME - endpoints are whitelable material as well. Should we have an `SdkClientOption` object in whitelable for some of these
    /**
     * Whether or not endpoint discovery is enabled or not. Default is true
     */
    val EndpointDiscoveryEnabled: ClientOption<Boolean> = ClientOption("EndpointDiscoveryEnabled")
}

/**
 * A collection of advanced options that can be configured on an AWS client.
 */
object AwsAdvancedClientOption {
    /**
     * Whether region detection should be enabled. Region detection is used when the region is not specified
     * when building a client. This is enabled by default.
     */
    val EnableDefaultRegionDetection: ClientOption<Boolean> = ClientOption("EnableDefaultRegionDetection")
}


// questionable whether we need this but it hides the "Attributes" property bag into something with (perhaps)
// more context specific names/interfaces

/**
 * Configure service client options manually
 */
interface ClientOptionsBuilder {
    /**
     * Check if the specified [option] exists
     */
    operator fun contains(option: ClientOption<*>): Boolean

    /**
     * Creates or changes an [option] with the specified [value]
     */
    fun<T: Any> set(option: ClientOption<T>, value: T)

    /**
     * Removes an option with the specified [option] if it exists
     */
    fun <T : Any> remove(option: ClientOption<T>)
}

private class ClientOptionsBuilderImpl(private val attributes: Attributes): ClientOptionsBuilder {
    override fun <T : Any> set(option: ClientOption<T>, value: T) {
        attributes[option] = value
    }
    override fun contains(option: ClientOption<*>): Boolean = attributes.contains(option)
    override fun <T : Any> remove(option: ClientOption<T>) = attributes.remove(option)
}

/**
 * Default service configuration most services will extend
 */
public abstract class AwsClientConfig protected constructor(builder: DslBuilder): AuthConfig, RegionConfig, HttpClientConfig {
    override val region: String? = builder.region
    override val signingRegion: String? = builder.signingRegion
    override val credentialProvider: CredentialsProvider? = builder.credentialProvider
    override val httpClientEngine: HttpClientEngine? = builder.httpClientEngine
    override val httpClientEngineConfig: HttpClientEngineConfig? = builder.httpClientEngineConfig

    // TODO - fluent builder for Java clients?
//    public interface Builder {
//        fun build(): AwsClientConfig
//    }

    public interface DslBuilder {
        var region: String?
        var signingRegion: String?
        var credentialProvider: CredentialsProvider?
        var httpClientEngine: HttpClientEngine?
        var httpClientEngineConfig: HttpClientEngineConfig?

        public fun options(block: ClientOptionsBuilder.() -> Unit)
    }

    public open class BuilderImpl() : DslBuilder {
        override var region: String? = null
        override var signingRegion: String? = null
        override var credentialProvider: CredentialsProvider? = null
        override var httpClientEngine: HttpClientEngine? = null
        override var httpClientEngineConfig: HttpClientEngineConfig? = null

        constructor(x: AwsClientConfig): this() {
            this.region = x.region
            this.signingRegion = x.signingRegion
            this.httpClientEngine = x.httpClientEngine
            this.httpClientEngineConfig = x.httpClientEngineConfig
            this.credentialProvider = x.credentialProvider
        }


        // additional options configured manually
        protected val manualOptions: Attributes = Attributes()
        override fun options(block: ClientOptionsBuilder.() -> Unit) {
            ClientOptionsBuilderImpl(manualOptions).apply(block)
        }
    }
}


abstract class AwsServiceClient {
    protected abstract val awsClientConfig: AwsClientConfig

    // TODO - service config
    protected fun resolveRegion(ctx: ExecutionContext): String {
        // favor the context if it's already set
        val regionFromCtx = ctx.getOrNull(AwsClientOption.Region)
        if (regionFromCtx != null) return regionFromCtx

        // use the default from the service config if configured
        if (awsClientConfig.region != null) return awsClientConfig.region!!

        // attempt to detect
        val allowDefaultRegionDetect = ctx.getOrNull(AwsAdvancedClientOption.EnableDefaultRegionDetection) ?: true
        if (!allowDefaultRegionDetect) {
            throw ClientException("No region was configured and region detection has been disabled")
        }

        TODO("implement default region detection")
    }
}

abstract class AwsHttpServiceClient : AwsServiceClient() {
    protected abstract val client: SdkHttpClient

    protected fun mergeServiceDefaults(ctx: ExecutionContext) {
        ctx.putIfAbsent(AwsClientOption.Region, resolveRegion(ctx))
        // TODO - update context to include defaults if not set already for remaining service config
    }
}

fun <T: Any> Attributes.putIfAbsent(key: AttributeKey<T>, value: T) {
    if (!contains(key)) set(key, value)
}

// TODO - does idempotency token generator/provider need to be an interface or can we just configure it as a client option?

/*

val client = Lambda.build {
    region = "us-east-1"
    signingRegion = "us-east-1"

    ...

    // will set context options not exposed directly as a property
    options {
        set(AuthOption.DoubleUriEncode, false)
    }
}

*/
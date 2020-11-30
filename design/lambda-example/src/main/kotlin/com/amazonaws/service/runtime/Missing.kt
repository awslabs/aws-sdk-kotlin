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

import software.aws.clientrt.client.ClientOptions
import software.aws.clientrt.client.ClientOptionsImpl
import software.aws.clientrt.client.ExecutionContext
import software.aws.clientrt.http.config.HttpClientConfig
import software.aws.clientrt.http.engine.HttpClientEngine
import software.aws.clientrt.http.engine.HttpClientEngineConfig
import software.aws.clientrt.util.Attributes
import software.aws.clientrt.util.putIfAbsent
import software.aws.kotlinsdk.ClientException
import software.aws.kotlinsdk.auth.AuthAttributes
import software.aws.kotlinsdk.auth.AuthConfig
import software.aws.kotlinsdk.auth.CredentialsProvider
import software.aws.kotlinsdk.client.AwsAdvancedClientOption
import software.aws.kotlinsdk.client.AwsClientOption
import software.aws.kotlinsdk.regions.RegionConfig

// ######################################################################################
// Things either missing or in-progress in the client runtime.
// ######################################################################################


/**
 * Default service configuration most services will extend
 */
public abstract class AwsClientConfig protected constructor(builder: DslBuilder): AuthConfig, RegionConfig, HttpClientConfig {
    // TODO - can we do this via property delegation the way we do in SdkOperation? AND allow derived classes and custom service
    // config properties to do the same? This would allow very easy extensibility and ensure that the context is _always_
    // configured
    // e.g. val region: String? by clientOption()
    // e.g. val myCustomProperty: Boolean? by requiredClientOption()

    override val region: String? = builder.region
    override val signingRegion: String? = builder.signingRegion
    override val credentialProvider: CredentialsProvider? = builder.credentialProvider
    override val httpClientEngine: HttpClientEngine? = builder.httpClientEngine
    override val httpClientEngineConfig: HttpClientEngineConfig? = builder.httpClientEngineConfig

    // TODO - fluent builder for Java clients?
    // public interface Builder {
    //     fun build(): AwsClientConfig
    // }

    public interface DslBuilder {
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
        var credentialProvider: CredentialsProvider?

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

        /**
         * Configure client options manually
         */
        public fun options(block: ClientOptions.() -> Unit)
    }

    public open class BuilderImpl() : DslBuilder {
        final override var region: String? = null
        final override var signingRegion: String? = null
        final override var credentialProvider: CredentialsProvider? = null
        final override var httpClientEngine: HttpClientEngine? = null
        final override var httpClientEngineConfig: HttpClientEngineConfig? = null

        constructor(x: AwsClientConfig): this() {
            this.region = x.region
            this.signingRegion = x.signingRegion
            this.httpClientEngine = x.httpClientEngine
            this.httpClientEngineConfig = x.httpClientEngineConfig
            this.credentialProvider = x.credentialProvider
        }


        // additional options configured manually
        protected val manualOptions: Attributes = Attributes()
        override fun options(block: ClientOptions.() -> Unit) {
            ClientOptionsImpl(manualOptions).apply(block)
        }
    }
}

/**
 * Default base class for AWS service clients
 * @param awsClientConfig common AWS service configuration
 */
abstract class AwsServiceClient(private val awsClientConfig: AwsClientConfig) {

    /**
     * Attempt to resolve the region to make requests to.
     *
     * Regions are resolved in the following order:
     *   1. From the existing [ctx]
     *   2. From the service config
     *   3. Using default region detection (only if-enabled)
     */
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

        TODO("default region detection has not been implemented yet")
    }

    // merge the defaults configured in [awsClientConfig] into the execution context before firing off a request
    protected fun mergeServiceDefaults(ctx: ExecutionContext) {
        val region = resolveRegion(ctx)
        ctx.putIfAbsent(AwsClientOption.Region, region)
        ctx.putIfAbsent(AuthAttributes.SigningRegion, awsClientConfig.signingRegion ?: region)
        // TODO - update context to include defaults if not set already for remaining service config

        // FIXME - we can't set the [AwsClientConfig.manualOptions] without either (1) a merge() like operation, or (2) using the
        // same Attributes() instance throughout. (1) breaks liskov substitution since it will require internal details of Attributes()
        // to implement and (2) requires careful API planning/consideration and tighter coupling

    }
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
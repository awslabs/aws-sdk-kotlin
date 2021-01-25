/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package com.amazonaws.service.lambda

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.auth.AuthAttributes
import aws.sdk.kotlin.runtime.auth.AwsSigv4Signer
import aws.sdk.kotlin.runtime.auth.DefaultChainCredentialsProvider
import aws.sdk.kotlin.runtime.regions.resolveRegionForOperation
import aws.sdk.kotlin.runtime.restjson.RestJsonError
import com.amazonaws.service.lambda.model.*
import com.amazonaws.service.lambda.transform.*
import kotlinx.coroutines.runBlocking
import software.aws.clientrt.SdkBaseException
import software.aws.clientrt.ServiceException
import software.aws.clientrt.client.ExecutionContext
import software.aws.clientrt.client.SdkClientOption
import software.aws.clientrt.config.IdempotencyTokenProvider
import software.aws.clientrt.http.*
import software.aws.clientrt.http.engine.HttpClientEngineConfig
import software.aws.clientrt.http.engine.ktor.KtorEngine
import software.aws.clientrt.http.feature.DefaultRequest
import software.aws.clientrt.http.feature.HttpSerde
import software.aws.clientrt.serde.json.JsonSerdeProvider
import software.aws.clientrt.util.InternalAPI
import software.aws.clientrt.util.putIfAbsent


@OptIn(InternalAPI::class)
class DefaultLambdaClient(private val config: LambdaClient.Config): LambdaClient {
    private val client: SdkHttpClient

    init {
        val engineConfig = HttpClientEngineConfig()
        val httpClientEngine = config.httpClientEngine ?: KtorEngine(engineConfig)

        client = sdkHttpClient(httpClientEngine) {
            install(HttpSerde) {
                serdeProvider = JsonSerdeProvider()
                idempotencyTokenProvider = config.idempotencyTokenProvider ?: IdempotencyTokenProvider.Default
            }

            // request defaults
            install(DefaultRequest) {
                url.scheme = Protocol.HTTP
                url.host = "127.0.0.1"
                url.port = 8000
            }

            install(RestJsonError) {
                // here is where we would register exception deserializers
            }

            install(AwsSigv4Signer) {
                credentialsProvider = config.credentialsProvider ?: DefaultChainCredentialsProvider()
            }
        }
    }

    /**
     * @throws ResourceNotFoundException
     * @throws TooManyRequestsException
     * @throws InvalidParameterValueException
     * @throws Ec2AccessDeniedException
     * @throws KmsAccessDeniedException
     * @throws ClientException
     * @throws ServiceException
     */
    override suspend fun invoke(input: InvokeRequest): InvokeResponse {
        val execCtx = SdkHttpOperation.build {
            serializer = InvokeRequestSerializer(input)
            deserializer = InvokeResponseDeserializer()
            service = serviceName
            operationName = "Invoke"
        }
        mergeServiceDefaults(execCtx)
        return client.roundTrip(execCtx)
    }

    /**
     * @throws InvalidParameterValueException
     * @throws ClientException
     * @throws ServiceException
     */
    override suspend fun createAlias(input: CreateAliasRequest): AliasConfiguration {
        val execCtx = SdkHttpOperation.build {
            serializer = CreateAliasRequestSerializer(input)
            deserializer = AliasConfigurationDeserializer()
            service = serviceName
            operationName = "CreateAlias"
        }
        mergeServiceDefaults(execCtx)
        return client.roundTrip(execCtx)
    }

    override fun close() {
        // TODO - whether we close this or not is dependent on whether we own the engine or not
        client.close()
    }

    // merge the defaults configured for the service into the execution context before firing off a request
    private fun mergeServiceDefaults(ctx: ExecutionContext) {
        val region = resolveRegionForOperation(ctx, config)
        ctx.putIfAbsent(aws.sdk.kotlin.runtime.client.AwsClientOption.Region, region)
        ctx.putIfAbsent(AuthAttributes.SigningRegion, config.signingRegion ?: region)
        ctx.putIfAbsent(SdkClientOption.ServiceName, serviceName)

        // ... any other service defaults
    }
}

fun main() = runBlocking{
    val client = LambdaClient {
    }

    val request = InvokeRequest {
        functionName = "myfunction"
        payload = "some payload".toByteArray()
    }

    println("running 'invoke' operation")
    val resp = client.invoke(request)
    println(resp)

    println("running 'createAlias' operation")
    val aliasConfig = client.createAlias {
        name = "LIVE"
        functionName = "my-function"
        functionVersion = "1"
        description = "alias for LIVE"
    }
    println(aliasConfig)

    println("running invalid 'createAlias' operation")
    try {
        client.createAlias {
            name = "DEV"
            description = "alias for DEV"
            // missing version
        }
    } catch (ex: SdkBaseException) {
        println("exception processing CreateAlias operation")
        println(ex)
    }

    // FIXME - why isn't this exiting...seems like OkHTTP engine dispatcher isn't closing?
    client.close()
}
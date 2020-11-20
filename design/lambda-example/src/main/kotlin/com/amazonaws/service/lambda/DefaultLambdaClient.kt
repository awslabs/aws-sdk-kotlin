/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package com.amazonaws.service.lambda

import com.amazonaws.service.lambda.model.*
import com.amazonaws.service.lambda.transform.*
import kotlinx.coroutines.runBlocking
import software.aws.clientrt.SdkBaseException
import software.aws.clientrt.ServiceException
import software.aws.clientrt.config.IdempotencyTokenProvider
import software.aws.clientrt.http.*
import software.aws.clientrt.http.ExecutionContext
import software.aws.clientrt.http.engine.HttpClientEngineConfig
import software.aws.clientrt.http.engine.ktor.KtorEngine
import software.aws.clientrt.http.feature.DefaultRequest
import software.aws.clientrt.http.feature.DefaultValidateResponse
import software.aws.clientrt.http.feature.HttpSerde
import software.aws.clientrt.serde.json.JsonSerdeProvider

class DefaultLambdaClient(config: LambdaClient.Config): LambdaClient {
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

            // this is what will be installed by the generic smithy-kotlin codegenerator
            install(DefaultValidateResponse)
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
        val execCtx = SdkOperation.build {
            serializer = InvokeRequestSerializer(input)
            deserializer = InvokeResponseDeserializer()
            service = serviceName
            operationName = "Invoke"
        }
        return client.roundTrip(execCtx)
    }

    /**
     * @throws InvalidParameterValueException
     * @throws ClientException
     * @throws ServiceException
     */
    override suspend fun createAlias(input: CreateAliasRequest): AliasConfiguration {
        val execCtx = SdkOperation.build {
            serializer = CreateAliasRequestSerializer(input)
            deserializer = AliasConfigurationDeserializer()
            service = serviceName
            operationName = "CreateAlias"
        }
        return client.roundTrip(execCtx)
    }

    override fun close() {
        client.close()
    }
}


fun main() = runBlocking{
    val client = LambdaClient()
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
/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.polly

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import aws.sdk.kotlin.services.polly.endpoints.DefaultEndpointProvider
import aws.sdk.kotlin.services.polly.endpoints.EndpointParameters
import aws.sdk.kotlin.services.polly.model.OutputFormat
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.sdk.kotlin.services.polly.model.VoiceId
import aws.sdk.kotlin.services.polly.presigners.PollyPresignConfig
import aws.sdk.kotlin.services.polly.presigners.presign
import aws.sdk.kotlin.testing.withAllEngines
import aws.smithy.kotlin.runtime.auth.awssigning.SigningContextualizedEndpoint
import aws.smithy.kotlin.runtime.http.SdkHttpClient
import aws.smithy.kotlin.runtime.http.response.complete
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for presigner
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PollyPresignerTest {

    @Test
    fun clientBasedPresign() = runBlocking {
        val request = SynthesizeSpeechRequest {
            voiceId = VoiceId.Salli
            outputFormat = OutputFormat.Pcm
            text = "hello world"
        }

        val client = PollyClient { region = "us-east-1" }
        val presignedRequest = request.presign(client.config, 10.seconds)

        withAllEngines { engine ->
            val httpClient = SdkHttpClient(engine)

            val call = httpClient.call(presignedRequest)
            call.complete()

            assertEquals(200, call.response.status.value, "presigned polly request failed for engine: $engine")
        }
    }

    @Test
    fun presignConfigBasedPresign() = runBlocking {
        val request = SynthesizeSpeechRequest {
            voiceId = VoiceId.Salli
            outputFormat = OutputFormat.Pcm
            text = "hello world"
        }

        val presignConfig = PollyPresignConfig {
            credentialsProvider = DefaultChainCredentialsProvider()
            endpointProvider = {
                val endpoint = DefaultEndpointProvider().resolveEndpoint(
                    EndpointParameters.invoke {
                        region = it.region
                    }
                )
                SigningContextualizedEndpoint(endpoint, it)
            }
            region = "us-east-1"
        }

        val presignedRequest = request.presign(presignConfig, 10.seconds)

        withAllEngines { engine ->
            val httpClient = SdkHttpClient(engine)

            val call = httpClient.call(presignedRequest)
            call.complete()

            assertEquals(200, call.response.status.value, "presigned polly request failed for engine: $engine")
        }
    }
}

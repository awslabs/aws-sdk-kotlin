/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.polly

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.polly.endpoints.DefaultEndpointProvider
import aws.sdk.kotlin.services.polly.endpoints.EndpointParameters
import aws.sdk.kotlin.services.polly.model.OutputFormat
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.sdk.kotlin.services.polly.model.VoiceId
import aws.sdk.kotlin.services.polly.presigners.PollyPresignConfig
import aws.sdk.kotlin.services.polly.presigners.presign
import aws.smithy.kotlin.runtime.auth.awssigning.SigningContextualizedEndpoint
import aws.smithy.kotlin.runtime.http.HttpMethod
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class PollyPresignerTest {

    @Test
    fun itProducesExpectedUrlComponents() = runTest {
        val request = SynthesizeSpeechRequest {
            voiceId = VoiceId.Salli
            outputFormat = OutputFormat.Pcm
            text = "hello world"
        }

        val clientConfig = PollyPresignConfig {
            region = "us-east-2"
            endpointProvider = { signingCtx ->
                val endpointParams = EndpointParameters {
                    region = "us-east-2"
                }
                val endpoint = DefaultEndpointProvider().resolveEndpoint(endpointParams)
                SigningContextualizedEndpoint(endpoint, signingCtx)
            }
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "AKID"
                secretAccessKey = "secret"
            }
        }

        val presignedRequest = request.presign(clientConfig, 10.seconds)

        assertEquals(HttpMethod.GET, presignedRequest.method)
        assertTrue(presignedRequest.headers.entries().size == 1)
        assertEquals("polly.us-east-2.amazonaws.com", presignedRequest.headers["Host"])
        assertEquals("/v1/speech", presignedRequest.url.path)
        val expectedQueryParameters = setOf("OutputFormat", "Text", "VoiceId", "X-Amz-Algorithm", "X-Amz-Credential", "X-Amz-Date", "X-Amz-SignedHeaders", "X-Amz-Expires", "X-Amz-Signature")
        assertEquals(expectedQueryParameters, presignedRequest.url.parameters.entries().map { it.key }.toSet())
    }
}

package aws.sdk.kotlin.services.polly

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.sdk.kotlin.services.polly.model.OutputFormat
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.sdk.kotlin.services.polly.model.VoiceId
import aws.smithy.kotlin.runtime.http.HttpMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PollyPresignerTest {

    @Test
    fun itProducesExpectedUrlComponents() = runSuspendTest {
        val request = SynthesizeSpeechRequest {
            voiceId = VoiceId.Salli
            outputFormat = OutputFormat.Pcm
            text = "hello world"
        }

        val clientConfig = PollyPresignConfig {
            region = "us-east-2"
            credentialsProvider = StaticCredentialsProvider {
                accessKeyId = "AKID"
                secretAccessKey = "secret"
            }
        }

        val presignedRequest = request.presign(clientConfig, 10)

        assertEquals(HttpMethod.GET, presignedRequest.method)
        assertTrue(presignedRequest.headers.entries().size == 1)
        assertEquals("polly.us-east-2.amazonaws.com", presignedRequest.headers["Host"])
        assertEquals("/v1/speech", presignedRequest.url.path)
        val expectedQueryParameters = setOf("OutputFormat", "Text", "VoiceId", "X-Amz-Algorithm", "X-Amz-Credential", "X-Amz-Date", "X-Amz-SignedHeaders", "X-Amz-Expires", "X-Amz-Signature")
        assertEquals(expectedQueryParameters, presignedRequest.url.parameters.entries().map { it.key }.toSet())
    }
}

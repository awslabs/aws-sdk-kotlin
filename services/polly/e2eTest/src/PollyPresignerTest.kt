package aws.sdk.kotlin.services.polly

import aws.sdk.kotlin.services.polly.model.OutputFormat
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.sdk.kotlin.services.polly.model.VoiceId
import aws.sdk.kotlin.services.polly.presigners.presign
import aws.sdk.kotlin.testing.withAllEngines
import aws.smithy.kotlin.runtime.http.response.complete
import aws.smithy.kotlin.runtime.http.sdkHttpClient
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
            val httpClient = sdkHttpClient(engine)

            val call = httpClient.call(presignedRequest)
            call.complete()

            assertEquals(200, call.response.status.value, "presigned polly request failed for engine: $engine")
        }
    }
}

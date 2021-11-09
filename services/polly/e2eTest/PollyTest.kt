package aws.sdk.kotlin.services.polly

import aws.sdk.kotlin.services.polly.model.OutputFormat
import aws.sdk.kotlin.services.polly.model.SynthesizeSpeechRequest
import aws.sdk.kotlin.services.polly.model.VoiceId
import aws.smithy.kotlin.runtime.http.request.HttpRequest
import aws.smithy.kotlin.runtime.testing.runSuspendTest
import org.junit.jupiter.api.TestInstance
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for presigner
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class STSIntegrationTest {

    @Test
    fun clientBasedPresign() = runSuspendTest {
        val request = SynthesizeSpeechRequest {
            voiceId = VoiceId.Salli
            outputFormat = OutputFormat.Pcm
            text = "hello world"
        }

        val client = PollyClient { region = "us-east-1" }
        val presignedRequest = request.presign(client.config, 10)

        val (code, _) = httpResponseFromGet(presignedRequest)

        assertEquals(200, code)
    }
}

fun httpResponseFromGet(presignedRequest: HttpRequest): Pair<Int, String?> {
    val url = URL(presignedRequest.url.toString())
    var urlConnection: HttpsURLConnection? = null
    try {
        urlConnection = url.openConnection() as HttpsURLConnection? ?: error("could not construct client")
        presignedRequest.headers.forEach { key, values ->
            urlConnection.setRequestProperty(key, values.first())
        }
        urlConnection.connect()

        if (urlConnection.errorStream != null) {
            error("PUT failed: ${urlConnection.errorStream?.bufferedReader()?.readText()}")
        }

        return urlConnection.responseCode to urlConnection.inputStream?.bufferedReader()?.readText()
    } finally {
        urlConnection!!.inputStream.close()
    }
}

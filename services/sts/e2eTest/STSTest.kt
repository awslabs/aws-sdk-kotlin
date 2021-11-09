package aws.sdk.kotlin.services.sts

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.smithy.kotlin.runtime.http.request.HttpRequest
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
    fun testPutObjectPresigner() = runSuspendTest {
        val c = StsClient { region = "us-east-2" }
        val req = GetCallerIdentityRequest { }
        val pr0 = req.presign(c.config, 60)

        val rc = httpResponseCodeFromGetPresignedUrl(pr0)

        assertEquals(200, rc)
    }
}

fun httpResponseCodeFromGetPresignedUrl(presignedRequest: HttpRequest): Int {
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
        return urlConnection.responseCode
    } finally {
        urlConnection!!.inputStream.close()
    }
}

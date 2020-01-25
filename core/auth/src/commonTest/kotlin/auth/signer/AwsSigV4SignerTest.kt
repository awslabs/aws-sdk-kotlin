package auth.signer

import com.soywiz.klock.ISO8601
import com.soywiz.klock.parse
import regions.Region
import types.AwsCredentials
import types.Headers
import types.HttpRequest
import types.RequestSigningArguments
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SERVICE = "foo"
private val REGION = Region.of("us-bar-1")
private val CREDENTIALS = AwsCredentials("foo", "bar")
private val REQUEST = HttpRequest(
    method = "POST",
    protocol = "https=",
    path = "/",
    headers = Headers(
        mutableMapOf(
            "host" to "foo.us-bar-1.amazonaws.com"
        )
    ),
    hostname = "foo.us-bar-1.amazonaws.com"
)

private val SIGNER = AwsSigV4Signer(SERVICE, REGION, CREDENTIALS)
private val DATE_TIME_FORMAT = ISO8601.BaseIsoDateTimeFormat("YYYY-MM-DDThh:mm:ssZ")

class AwsSigV4RequestSignerTest {
    @Test
    fun requestWithNoBody() {
        val headers = SIGNER.sign(REQUEST, RequestSigningArguments(DATE_TIME_FORMAT.parse("2000-01-01T00:00:00").utc)).headers

        assertEquals(message = null, actual = headers["Authorization"], expected = "AWS4-HMAC-SHA256 Credential=foo/20000101/us-bar-1/foo/aws4_request, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=1e3b24fcfd7655c0c245d99ba7b6b5ca6174eab903ebfbda09ce457af062ad30")
    }

    private fun assertSignatureMatches(request: HttpRequest, expectedSignature: String) {

    }
}
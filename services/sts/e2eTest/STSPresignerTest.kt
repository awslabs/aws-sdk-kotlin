package aws.sdk.kotlin.services.sts

import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.sdk.kotlin.services.sts.presigners.presign
import aws.sdk.kotlin.testing.withAllEngines
import aws.smithy.kotlin.runtime.http.response.complete
import aws.smithy.kotlin.runtime.http.sdkHttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/**
 * Tests for presigner
 */
@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StsPresignerTest {

    @Test
    fun testGetCallerIdentityPresigner() = runBlocking {
        val c = StsClient { region = "us-east-2" }
        val req = GetCallerIdentityRequest { }
        val presignedRequest = req.presign(c.config, 60.seconds)

        withAllEngines { engine ->
            val httpClient = sdkHttpClient(engine)

            val call = httpClient.call(presignedRequest)
            call.complete()

            assertEquals(200, call.response.status.value, "presigned sts request failed for engine: $engine")
        }
    }
}

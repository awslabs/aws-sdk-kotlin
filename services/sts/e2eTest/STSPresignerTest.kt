package aws.sdk.kotlin.services.sts

import aws.sdk.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.sdk.kotlin.services.sts.presigners.presign
import aws.smithy.kotlin.runtime.http.response.complete
import aws.smithy.kotlin.runtime.http.sdkHttpClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance
import kotlin.test.Ignore
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
    @Ignore // FIXME: CRT HTTP client fails to get connection after kotlinx-coroutines-test 1.6.0 upgrade
    fun testGetCallerIdentityPresigner() = runTest {
        val c = StsClient { region = "us-east-2" }
        val req = GetCallerIdentityRequest { }
        val presignedRequest = req.presign(c.config, 60.seconds)

        CrtHttpEngine().use { engine ->
            val httpClient = sdkHttpClient(engine)

            val call = httpClient.call(presignedRequest)
            call.complete()

            assertEquals(200, call.response.status.value)
        }
    }
}

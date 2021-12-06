package aws.sdk.kotlin.services.sts

import aws.sdk.kotlin.runtime.http.engine.crt.CrtHttpEngine
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.sdk.kotlin.services.sts.model.GetCallerIdentityRequest
import aws.smithy.kotlin.runtime.http.response.complete
import aws.smithy.kotlin.runtime.http.sdkHttpClient
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for presigner
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StsPresignerTest {

    @Test
    fun testGetCallerIdentityPresigner() = runSuspendTest {
        val c = StsClient { region = "us-east-2" }
        val req = GetCallerIdentityRequest { }
        val presignedRequest = req.presign(c.config, 60)

        CrtHttpEngine().use { engine ->
            val httpClient = sdkHttpClient(engine)

            val call = httpClient.call(presignedRequest)
            call.complete()

            assertEquals(200, call.response.status.value)
        }
    }
}

import aws.sdk.kotlin.test.checksums.*
import kotlin.test.Test

class ClientConfigTests {
    @Test
    fun test() {
        TestClient {}.use { client -> client.close() }
    }
}
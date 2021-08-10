package aws.sdk.kotlin.e2etest

import kotlin.test.Test

abstract class E2eServiceTest {
}

class S3IntegrationTest {
    @Test
    fun testPutObject() {
        println("hello e2e tests")
    }

}
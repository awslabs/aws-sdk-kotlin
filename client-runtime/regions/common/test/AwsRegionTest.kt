package software.aws.kotlinsdk.regions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AwsRegionTest {

    @Test
    fun `it can resolve an AwsRegion from a string`() {
        val region = awsRegionByIdResolver("us-east-1")

        assertNotNull(region)
        assertEquals(region.id, "us-east-1")
    }
}

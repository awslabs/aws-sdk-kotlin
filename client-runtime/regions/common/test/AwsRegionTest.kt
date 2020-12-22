package software.aws.kotlinsdk.regions

import software.aws.clientrt.client.ExecutionContext
import software.aws.kotlinsdk.client.AwsClientOption
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

    @Test
    fun `it resolves region for operation`() {
        // from context
        val config = object : RegionConfig {
            override val region: String? = "us-west-2"
        }

        val actual = resolveRegionForOperation(ctx = ExecutionContext().apply { set(AwsClientOption.Region, "us-east-1") }, config)
        assertEquals("us-east-1", actual)

        // from config
        val actual2 = resolveRegionForOperation(ExecutionContext(), config)
        assertEquals("us-west-2", actual2)

        // TODO - from default region discovery
    }
}

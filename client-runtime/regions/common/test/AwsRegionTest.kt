package aws.sdk.kotlin.runtime.regions

import aws.sdk.kotlin.runtime.InternalSdkApi
import software.aws.clientrt.client.ExecutionContext
import aws.sdk.kotlin.runtime.client.AwsClientOption
import kotlin.test.Test
import kotlin.test.assertEquals

@InternalSdkApi
class AwsRegionTest {

    @Test
    fun `it resolves region for operation`() {
        // from context
        val config = object : RegionConfig {
            override val region: String = "us-west-2"
        }

        val actual = resolveRegionForOperation(ctx = ExecutionContext().apply { set(AwsClientOption.Region, "us-east-1") }, config)
        assertEquals("us-east-1", actual)

        // from config
        val actual2 = resolveRegionForOperation(ExecutionContext(), config)
        assertEquals("us-west-2", actual2)

        // TODO - from default region discovery
    }
}

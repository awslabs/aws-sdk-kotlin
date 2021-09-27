package aws.sdk.kotlin.runtime

import aws.smithy.kotlin.runtime.util.Platform
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AwsSdkSettingTest {

    @Test
    fun itLoadsJVMSettingFirst() {
        val testPlatform = mockPlatform(mapOf("AWS_PROFILE" to "env"), mapOf("aws.profile" to "jvm"))

        val actual = AwsSdkSetting.AwsProfile.resolve(testPlatform)

        assertEquals("jvm", actual)
    }

    @Test
    fun itLoadsEnvSettingSecond() {
        val testPlatform = mockPlatform(mapOf("AWS_PROFILE" to "env"), mapOf())

        val actual = AwsSdkSetting.AwsProfile.resolve(testPlatform)

        assertEquals("env", actual)
    }

    @Test
    fun itLoadsDefaultSettingThird() {
        val testPlatform = mockPlatform(mapOf(), mapOf())

        val actual = AwsSdkSetting.AwsProfile.resolve(testPlatform)

        assertEquals("default", actual)
    }

    @Test
    fun itReturnsNullWithNoValue() {
        val testPlatform = mockPlatform(mapOf(), mapOf())

        val actual = AwsSdkSetting.AwsAccessKeyId.resolve(testPlatform)

        assertNull(actual)
    }

    private fun mockPlatform(env: Map<String, String>, jvmProps: Map<String, String>): Platform {
        val testPlatform = mockk<Platform>()
        val envKeyParam = slot<String>()
        val propKeyParam = slot<String>()

        every { testPlatform.getenv(capture(envKeyParam)) } answers {
            env[envKeyParam.captured]
        }
        every { testPlatform.getProperty(capture(propKeyParam)) } answers {
            jvmProps[propKeyParam.captured]
        }
        return testPlatform
    }
}

package aws.sdk.kotlin.runtime

import aws.smithy.kotlin.runtime.util.Platform
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AwsSdkSettingTest {

    @Test
    fun itLoadsJVMSettingFirst() {
        mockPlatform(mapOf("AWS_PROFILE" to "env"), mapOf("aws.profile" to "jvm"))

        val actual = AwsSdkSetting.AwsProfile.resolve()

        assertEquals("jvm", actual)
    }

    @Test
    fun itLoadsEnvSettingSecond() {
        mockPlatform(mapOf("AWS_PROFILE" to "env"), mapOf())

        val actual = AwsSdkSetting.AwsProfile.resolve()

        assertEquals("env", actual)
    }

    @Test
    fun itLoadsDefaultSettingThird() {
        mockPlatform(mapOf(), mapOf())

        val actual = AwsSdkSetting.AwsProfile.resolve()

        assertEquals("default", actual)
    }

    @Test
    fun itReturnsNullWithNoValue() {
        mockPlatform(mapOf(), mapOf())

        val actual = AwsSdkSetting.AwsAccessKeyId.resolve()

        assertNull(actual)
    }

    private fun mockPlatform(env: Map<String, String>, jvmProps: Map<String, String>) {
        mockkObject(Platform)
        val envKeyParam = slot<String>()
        val propKeyParam = slot<String>()

        every { Platform.getenv(capture(envKeyParam)) } answers {
            env[envKeyParam.captured]
        }
        every { Platform.getProperty(capture(propKeyParam)) } answers {
            jvmProps[propKeyParam.captured]
        }
    }
}

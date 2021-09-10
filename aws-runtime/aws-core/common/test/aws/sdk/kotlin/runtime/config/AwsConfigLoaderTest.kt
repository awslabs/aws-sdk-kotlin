package aws.sdk.kotlin.runtime.config

import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.Platform
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AwsConfigLoaderTest {

    @Test
    fun canPassTestSuite() {
        val testCases = Json.parseJson(loaderTestSuiteJson).jsonObject["tests"]!!.jsonArray

        testCases
            .map { TestCase.fromJson(it.jsonObject) }
            // .filter { testCase -> testCase.name == "User home is loaded from HOME with highest priority on windows platforms." }
            .forEachIndexed { index, testCase ->
                val testPlatform = mockPlatform(testCase)

                val actual = resolveConfigSource(testPlatform)

                if (testCase.profile != null) {
                    assertEquals(testCase.profile, actual.profile)
                } else {
                    assertTrue(actual.profile == "default") // test cases that do not define profiles should use default
                }
                assertEquals(testCase.profile, if (testCase.profile != null) actual.profile else null)
                assertEquals(testCase.configLocation, actual.configPath)
                assertEquals(testCase.credentialsLocation, actual.credentialsPath)
            }
    }

    @Test
    fun itLoadsAWSConfigurationWithCustomProfile() {
        val testPlatform = mockPlatform(
            pathSegment = "/",
            awsProfileEnv = "bob",
            homeEnv = "/home/user",
            os = OperatingSystem(OsFamily.Linux, null)
        )

        val config = loadAwsConfiguration(testPlatform)

        assertTrue(config.profileName == "bob")
        assertTrue(config.isEmpty())
    }

    @Test
    fun configurationLoadingDoesNotThrowErrors() {
        val activeProfile = loadAwsConfiguration()

        assertTrue(activeProfile.profileName.isNotBlank())
    }

    internal fun mockPlatform(
        pathSegment: String,
        awsProfileEnv: String? = null,
        awsConfigFileEnv: String? = null,
        homeEnv: String? = null,
        awsSharedCredentialsFileEnv: String? = null,
        homeProp: String? = null,
        os: OperatingSystem
    ): Platform {
        val testPlatform = mockk<Platform>()
        val envKeyParam = slot<String>()
        val propKeyParam = slot<String>()

        every { testPlatform.filePathSegment } returns pathSegment
        every { testPlatform.getenv(capture(envKeyParam)) } answers {
            when (envKeyParam.captured) {
                "AWS_PROFILE" -> awsProfileEnv
                "AWS_CONFIG_FILE" -> awsConfigFileEnv
                "HOME" -> homeEnv
                "AWS_SHARED_CREDENTIALS_FILE" -> awsSharedCredentialsFileEnv
                else -> error(envKeyParam.captured)
            }
        }
        every { testPlatform.getProperty(capture(propKeyParam)) } answers {
            if (propKeyParam.captured == "user.home") homeProp else null
        }
        every { testPlatform.osInfo() } returns os

        return testPlatform
    }

    private fun mockPlatform(testCase: TestCase): Platform {
        val testPlatform = mockk<Platform>()
        val envKeyParam = slot<String>()
        val propKeyParam = slot<String>()

        every { testPlatform.filePathSegment } returns when (testCase.platform) {
            OsFamily.Windows -> "\\"
            else -> "/"
        }
        every { testPlatform.getenv(capture(envKeyParam)) } answers {
            (testCase.environment[envKeyParam.captured] as JsonLiteral?)?.content
        }
        every { testPlatform.getProperty(capture(propKeyParam)) } answers {
            if (propKeyParam.captured == "user.home") testCase.languageSpecificHome else null
        }
        every { testPlatform.osInfo() } returns OperatingSystem(testCase.platform, null)

        return testPlatform
    }

    private data class TestCase(
        val name: String,
        val environment: Map<String, JsonElement>,
        val languageSpecificHome: String?,
        val platform: OsFamily,
        val profile: String?,
        val configLocation: String,
        val credentialsLocation: String
    ) {
        companion object {
            fun fromJson(json: JsonObject): TestCase {
                val name = (json["name"] as JsonLiteral).content
                val environment: Map<String, JsonElement> = json["environment"] as JsonObject
                val languageSpecificHome = (json["languageSpecificHome"] as JsonLiteral?)?.content
                val platformRaw = (json["platform"] as JsonLiteral).content
                val profile = (json["profile"] as JsonLiteral?)?.content
                val configLocation = (json["configLocation"] as JsonLiteral).content
                val credentialsLocation = (json["credentialsLocation"] as JsonLiteral).content

                val platform = when (platformRaw) {
                    "windows" -> OsFamily.Windows
                    "linux" -> OsFamily.Linux
                    else -> error("Unexpected platform $platformRaw")
                }

                return TestCase(
                    name,
                    environment,
                    languageSpecificHome,
                    platform,
                    profile,
                    configLocation,
                    credentialsLocation
                )
            }
        }
    }
}

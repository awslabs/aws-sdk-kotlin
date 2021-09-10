package aws.sdk.kotlin.runtime.auth.config

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
        val testList = Json.parseJson(testSuiteJson).jsonObject["tests"]!!.jsonArray

        testList
            .map { TestCase.fromJson(it.jsonObject) }
            // .filter { testCase -> testCase.name == "User home is loaded from HOME with highest priority on windows platforms." }
            .forEachIndexed { index, testCase ->
                val testPlatform = setupPlatformMock(testCase)

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
        val testPlatform = mockk<Platform>()
        val envKeyParam = slot<String>()
        val propKeyParam = slot<String>()

        every { testPlatform.filePathSegment } returns "/"
        every { testPlatform.getenv(capture(envKeyParam)) } answers {
            when (envKeyParam.captured) {
                "AWS_PROFILE" -> "bob"
                "AWS_CONFIG_FILE" -> null
                "HOME" -> null
                "AWS_SHARED_CREDENTIALS_FILE" -> null
                else -> error(envKeyParam.captured)
            }
        }
        every { testPlatform.getProperty(capture(propKeyParam)) } answers {
            if (propKeyParam.captured == "user.home") "/home/user" else null
        }
        every { testPlatform.osInfo() } returns OperatingSystem(OsFamily.Linux, null)

        val config = loadAwsConfiguration(testPlatform)

        assertTrue(config.profileName == "bob")
        assertTrue(config.isEmpty())
    }

    private fun setupPlatformMock(testCase: TestCase): Platform {
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

    @Test
    fun configurationLoadingDoesNotThrowErrors() {
        val activeProfile = loadAwsConfiguration()

        assertTrue(activeProfile.profileName.isNotBlank())
    }

    private val testSuiteJson = """
        {
          "description": [
            "These are test descriptions that specify which files and profiles should be loaded based on the specified environment ",
            "variables.",
            "See 'file-location-tests.schema.json' for a description of this file's structure."
          ],
        
          "tests": [
            {
              "name": "User home is loaded from HOME with highest priority on non-windows platforms.",
              "environment": {
                "HOME": "/home/user",
                "USERPROFILE": "ignored",
                "HOMEDRIVE": "ignored",
                "HOMEPATH": "ignored"
              },
              "languageSpecificHome": "ignored",
              "platform": "linux",
              "profile": "default",
              "configLocation": "/home/user/.aws/config",
              "credentialsLocation": "/home/user/.aws/credentials"
            },
        
            {
              "name": "User home is loaded using language-specific resolution on non-windows platforms when HOME is not set.",
              "environment": {
                "USERPROFILE": "ignored",
                "HOMEDRIVE": "ignored",
                "HOMEPATH": "ignored"
              },
              "languageSpecificHome": "/home/user",
              "platform": "linux",
              "profile": "default",
              "configLocation": "/home/user/.aws/config",
              "credentialsLocation": "/home/user/.aws/credentials"
            },
        
            {
              "name": "User home is loaded from HOME with highest priority on windows platforms.",
              "environment": {
                "HOME": "C:\\users\\user",
                "USERPROFILE": "ignored",
                "HOMEDRIVE": "ignored",
                "HOMEPATH": "ignored"
              },
              "languageSpecificHome": "ignored",
              "platform": "windows",
              "profile": "default",
              "configLocation": "C:\\users\\user\\.aws\\config",
              "credentialsLocation": "C:\\users\\user\\.aws\\credentials"
            },
        
            {
              "name": "User home is loaded from USERPROFILE on windows platforms when HOME is not set.",
              "environment": {
                "USERPROFILE": "C:\\users\\user",
                "HOMEDRIVE": "ignored",
                "HOMEPATH": "ignored"
              },
              "languageSpecificHome": "ignored",
              "platform": "windows",
              "profile": "default",
              "configLocation": "C:\\users\\user\\.aws\\config",
              "credentialsLocation": "C:\\users\\user\\.aws\\credentials"
            },
        
            {
              "name": "User home is loaded from HOMEDRIVEHOMEPATH on windows platforms when HOME and USERPROFILE are not set.",
              "environment": {
                "HOMEDRIVE": "C:",
                "HOMEPATH": "\\users\\user"
              },
              "languageSpecificHome": "ignored",
              "platform": "windows",
              "profile": "default",
              "configLocation": "C:\\users\\user\\.aws\\config",
              "credentialsLocation": "C:\\users\\user\\.aws\\credentials"
            },
        
            {
              "name": "User home is loaded using language-specific resolution on windows platforms when no environment variables are set.",
              "environment": {
              },
              "languageSpecificHome": "C:\\users\\user",
              "platform": "windows",
              "profile": "default",
              "configLocation": "C:\\users\\user\\.aws\\config",
              "credentialsLocation": "C:\\users\\user\\.aws\\credentials"
            },
        
            {
              "name": "The default config location can be overridden by the user on non-windows platforms.",
              "environment": {
                "AWS_CONFIG_FILE": "/other/path/config",
                "HOME": "/home/user"
              },
              "platform": "linux",
              "configLocation": "/other/path/config",
              "credentialsLocation": "/home/user/.aws/credentials"
            },
        
            {
              "name": "The default credentials location can be overridden by the user on non-windows platforms.",
              "environment": {
                "AWS_SHARED_CREDENTIALS_FILE": "/other/path/credentials",
                "HOME": "/home/user"
              },
              "platform": "linux",
              "profile": "default",
              "configLocation": "/home/user/.aws/config",
              "credentialsLocation": "/other/path/credentials"
            },
        
            {
              "name": "The default credentials location can be overridden by the user on windows platforms.",
              "environment": {
                "AWS_CONFIG_FILE": "C:\\other\\path\\config",
                "HOME": "C:\\users\\user"
              },
              "platform": "windows",
              "profile": "default",
              "configLocation": "C:\\other\\path\\config",
              "credentialsLocation": "C:\\users\\user\\.aws\\credentials"
            },
        
            {
              "name": "The default credentials location can be overridden by the user on windows platforms.",
              "environment": {
                "AWS_SHARED_CREDENTIALS_FILE": "C:\\other\\path\\credentials",
                "HOME": "C:\\users\\user"
              },
              "platform": "windows",
              "profile": "default",
              "configLocation": "C:\\users\\user\\.aws\\config",
              "credentialsLocation": "C:\\other\\path\\credentials"
            },
        
            {
              "name": "The default profile can be overridden via environment variable.",
              "environment": {
                "AWS_PROFILE": "other",
                "HOME": "/home/user"
              },
              "platform": "linux",
              "profile": "other",
              "configLocation": "/home/user/.aws/config",
              "credentialsLocation": "/home/user/.aws/credentials"
            }
          ]
        }        
    """.trimIndent()
}

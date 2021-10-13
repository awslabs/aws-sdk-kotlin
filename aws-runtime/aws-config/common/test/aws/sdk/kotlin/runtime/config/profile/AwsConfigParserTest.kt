/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.config.profile

import aws.sdk.kotlin.runtime.testing.runSuspendTest
import aws.smithy.kotlin.runtime.util.OperatingSystem
import aws.smithy.kotlin.runtime.util.OsFamily
import aws.smithy.kotlin.runtime.util.Platform
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AwsProfileParserTest {

    @Test
    fun canPassTestSuite() {
        val testList = Json.parseToJsonElement(parserTestSuiteJson).jsonObject["tests"]!!.jsonArray

        testList
            .map { TestCase.fromJson(it.jsonObject) }
            // .filter { testCase ->
            // testCase is TestCase.MatchConfigOutputCase &&
            // testCase.name == "Property values can be continued on the next line."
            // }
            .forEachIndexed { index, testCase ->
                when (testCase) {
                    is TestCase.MatchConfigOutputCase -> {
                        val actual = parse(FileType.CONFIGURATION, testCase.configInput).toJsonElement()
                        val expectedJson = Json.parseToJsonElement(testCase.expectedOutput)
                        assertEquals(expectedJson, actual, message = "[idx=$index]: $testCase")
                    }
                    is TestCase.MatchCredentialOutputCase -> {
                        val actual = parse(FileType.CREDENTIAL, testCase.credentialInput).toJsonElement()
                        assertEquals(testCase.expectedOutput, actual.toString(), message = "[idx=$index]: $testCase")
                    }
                    is TestCase.MatchConfigAndCredentialOutputCase -> {
                        val actual = loadConfiguration({ testCase.configInput }, { testCase.credentialInput }).toJsonElement()
                        assertEquals(testCase.expectedOutput, actual.toString(), message = "[idx=$index]: $testCase")
                    }
                    is TestCase.MatchErrorCase -> {
                        assertFails { parse(FileType.CONFIGURATION, testCase.input) }
                    }
                }
            }
    }

    @Test
    fun itCanBeUsedInTests() = runSuspendTest {
        // NOTE: This is the minimal mock of the Platform type needed to support aws configuration loading of a specific kvp.
        val testPlatform = mockk<Platform>()
        val propKeyParam = slot<String>()
        val filePath = slot<String>()

        every { testPlatform.getenv(any()) } answers { null }
        every { testPlatform.getProperty(capture(propKeyParam)) } answers { if (propKeyParam.captured == "user.home") "/home" else null }
        every { testPlatform.filePathSeparator } returns "/"
        every { testPlatform.osInfo() } returns OperatingSystem(OsFamily.Linux, null)
        coEvery { testPlatform.readFileOrNull(capture(filePath)) } answers {
            if (filePath.captured == "/home/.aws/config") "[profile default]\nboo = hoo".encodeToByteArray() else null
        }

        assertEquals("hoo", fnThatLoadsConfiguration(testPlatform))
    }

    /**
     * Example function that reads the active provide and returns true if a key "boo" exists.
     */
    private suspend fun fnThatLoadsConfiguration(platform: Platform): String? {
        val profile = loadActiveAwsProfile(platform)

        return profile["boo"]
    }

    @Test
    fun itCanMergeUniqueProfiles() {
        val m1 = mapOf(
            "a" to mapOf("x" to "1"),
        )
        val m2 = mapOf(
            "b" to mapOf("y" to "1")
        )

        val expected = mapOf(
            "a" to mapOf("x" to "1"),
            "b" to mapOf("y" to "1")
        )

        val actual = mergeProfiles(m1, m2)

        assertEquals(expected, actual)
    }

    @Test
    fun itCanMergeOverlappingProfiles() {
        val m1 = mapOf(
            "a" to mapOf("x" to "1"),
        )
        val m2 = mapOf(
            "a" to mapOf("z" to "1"),
            "b" to mapOf("y" to "1"),
        )

        val expected = mapOf(
            "a" to mapOf(
                "x" to "1",
                "z" to "1"
            ),
            "b" to mapOf("y" to "1")
        )

        val actual = mergeProfiles(m1, m2)

        assertEquals(expected, actual)
    }
    @Test
    fun lastMapWinsMergingInProfiles() {
        val m1 = mapOf(
            "a" to mapOf("x" to "1"),
        )
        val m2 = mapOf(
            "a" to mapOf("x" to "2"),
        )

        val expected = mapOf(
            "a" to mapOf("x" to "2")
        )

        val actual = mergeProfiles(m1, m2)

        assertEquals(expected, actual)
    }

    private sealed class TestCase {
        companion object {
            fun fromJson(json: JsonObject): TestCase {
                val name = (json["name"] as JsonPrimitive).content
                val configIn = (json["input"]!!.jsonObject["configFile"] as JsonPrimitive?)?.content
                val credentialIn = (json["input"]!!.jsonObject["credentialsFile"] as JsonPrimitive?)?.content
                val expected = json["output"]!!.jsonObject["profiles"]?.toString()
                val errorContaining = (json["output"]!!.jsonObject["errorContaining"] as JsonPrimitive?)?.content

                check(expected != null || errorContaining != null) { "Unexpected output: $json" }
                check(configIn != null || credentialIn != null) { "Unexpected output: $json" }

                val isErrorCase = expected == null && errorContaining != null

                return if (!isErrorCase) {
                    when {
                        configIn != null && credentialIn != null -> MatchConfigAndCredentialOutputCase(name, configIn, credentialIn, expected!!)
                        configIn != null -> MatchConfigOutputCase(name, configIn, expected!!)
                        credentialIn != null -> MatchCredentialOutputCase(name, credentialIn, expected!!)
                        else -> error("Unexpected branch from $json")
                    }
                } else {
                    MatchErrorCase(name, configIn!!, errorContaining!!)
                }
            }
        }

        data class MatchConfigOutputCase(val name: String, val configInput: String, val expectedOutput: String) :
            TestCase()

        data class MatchCredentialOutputCase(val name: String, val credentialInput: String, val expectedOutput: String) :
            TestCase()

        data class MatchConfigAndCredentialOutputCase(val name: String, val configInput: String, val credentialInput: String, val expectedOutput: String) :
            TestCase()

        data class MatchErrorCase(
            val name: String,
            val input: String,
            val expectedErrorMessage: String
        ) : TestCase()
    }

    /**
     * Produces a merged AWS configuration based on optional configuration and credential file contents.
     * @param configurationFn a function that will retrieve a configuration file as a UTF-8 string.
     * @param credentialsFn a function that will retrieve a configuration file as a UTF-8 string.
     * @return A map containing all specified profiles defined in configuration and credential files.
     */
    private fun loadConfiguration(configurationFn: () -> String?, credentialsFn: () -> String?): ProfileMap =
        mergeProfiles(
            parse(FileType.CONFIGURATION, configurationFn()),
            parse(FileType.CREDENTIAL, credentialsFn()),
        )
}

// See https://youtrack.jetbrains.com/issue/KTOR-3063
private fun Map<*, *>.toJsonElement(): JsonElement {
    val map: MutableMap<String, JsonElement> = mutableMapOf()
    this.forEach { (key, value) ->
        key as String
        when (value) {
            null -> map[key] = JsonNull
            is Map<*, *> -> map[key] = value.toJsonElement()
            is Boolean -> map[key] = JsonPrimitive(value)
            is Number -> map[key] = JsonPrimitive(value)
            is String -> map[key] = JsonPrimitive(value)
            is Enum<*> -> map[key] = JsonPrimitive(value.toString())
            else -> throw IllegalStateException("Can't serialize unknown type: $value")
        }
    }
    return JsonObject(map)
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.region

import aws.sdk.kotlin.runtime.testing.TestPlatformProvider
import aws.sdk.kotlin.runtime.testing.runSuspendTest
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultRegionProviderChainTest {
    private data class RegionProviderChainTest(
        val name: String,
        val platformProvider: TestPlatformProvider,
        val region: String?,
        val targets: List<String> = emptyList()
    )

    @Test
    fun testSuite(): Unit = runSuspendTest {
        val tests = Json.parseToJsonElement(regionProviderChainTestSuite).jsonArray
            .map { it.jsonObject }
            .map {
                val name = it["name"]!!.jsonPrimitive.content
                val platform = TestPlatformProvider.fromJsonNode(it["platform"]!!.jsonObject)
                val region = it["region"]!!.jsonPrimitive.contentOrNull
                RegionProviderChainTest(name, platform, region)
            }

        tests.forEach { test ->
            val provider = DefaultRegionProviderChain(test.platformProvider)
            val actual = provider.getRegion()
            assertEquals(test.region, actual, test.name)
        }
    }
}

/**
 * Construct a [TestPlatformProvider] from a JSON node like:
 *
 * ```json
 * {
 *     "env": {
 *         "ENV_VAR": "value"
 *     },
 *     "props": {
 *         "aws.property": "value"
 *     },
 *     "fs": {
 *         "filename": "contents"
 *     }
 * }
 * ```
 */
fun TestPlatformProvider.Companion.fromJsonNode(obj: JsonObject): TestPlatformProvider {
    val env = obj["env"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
    val props = obj["props"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
    val fs = obj["fs"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
    return TestPlatformProvider(env, props, fs)
}

// language=JSON
private const val regionProviderChainTestSuite = """
[
    {
        "name": "no region configured",
        "platform": {
            "env": {},
            "props": {},
            "fs": {}
        },
        "region": null
    },
    {
        "name": "environment configured",
        "platform": {
            "env": {
                "AWS_REGION": "us-east-2"
            },
            "props": {},
            "fs": {}
        },
        "region": "us-east-2"
    },
    {
        "name": "jvm property is favored",
        "platform": {
            "env": {
                "AWS_REGION": "us-east-2"
            },
            "props": {
                "aws.region": "us-west-1"
            },
            "fs": {}
        },
        "region": "us-west-1"
    },
    {
        "name": "default profile",
        "platform": {
            "env": {
              "AWS_CONFIG_FILE": "config"
            },
            "props": {},
            "fs": {
              "config": "[default]\nregion = us-east-2"
            }
        },
        "region": "us-east-2"
    },
    {
        "name": "explicit profile",
        "platform": {
            "env": {
              "AWS_CONFIG_FILE": "config",
              "AWS_PROFILE": "test-profile"
            },
            "props": {},
            "fs": {
              "config": "[default]\nregion = us-east-2\n[profile test-profile]\nregion = us-west-1"
            }
        },
        "region": "us-west-1"
    }
]
"""

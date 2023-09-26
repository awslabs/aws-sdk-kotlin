/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.auth.credentials

import aws.smithy.kotlin.runtime.serde.json.JsonDeserializer
import aws.smithy.kotlin.runtime.time.Instant
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonCredentialsDeserializerTest {
    @Test
    fun testSuccessResponse() = runTest {
        val payload = """
        {
            "Code" : "Success",
            "LastUpdated" : "2021-09-17T20:57:08Z",
            "Type" : "AWS-HMAC",
            "AccessKeyId" : "ASIARTEST",
            "SecretAccessKey" : "xjtest",
            "Token" : "IQote///test",
            "Expiration" : "2021-09-18T03:31:56Z"
        }
        """.trimIndent()
        val deserializer = JsonDeserializer(payload.encodeToByteArray())

        val parsed = deserializeJsonCredentials(deserializer)
        val expected = JsonCredentialsResponse.SessionCredentials(
            "ASIARTEST",
            "xjtest",
            "IQote///test",
            Instant.fromEpochSeconds(1631935916),
        )
        assertEquals(expected, parsed)
    }

    @Test
    fun testInvalidJson() = runTest {
        val payload = "404: not found"
        val deserializer = JsonDeserializer(payload.encodeToByteArray())

        val ex = assertFailsWith<InvalidJsonCredentialsException> {
            deserializeJsonCredentials(deserializer)
        }
        ex.message.shouldContain("invalid JSON credentials response")
    }

    @Test
    fun testSuccessResponseMissingCode() = runTest {
        val payload = """
        {
            "LastUpdated" : "2021-09-17T20:57:08Z",
            "Type" : "AWS-HMAC",
            "AccessKeyId" : "ASIARTEST",
            "SecretAccessKey" : "xjtest",
            "Token" : "IQote///test",
            "Expiration" : "2021-09-18T03:31:56Z"
        }
        """.trimIndent()
        val deserializer = JsonDeserializer(payload.encodeToByteArray())

        val parsed = deserializeJsonCredentials(deserializer)
        val expected = JsonCredentialsResponse.SessionCredentials(
            "ASIARTEST",
            "xjtest",
            "IQote///test",
            Instant.fromEpochSeconds(1631935916),
        )
        assertEquals(expected, parsed)
    }

    @Test
    fun testMissingAccessKeyId() = runTest {
        val payload = """
        {
            "LastUpdated" : "2021-09-17T20:57:08Z",
            "Type" : "AWS-HMAC",
            "SecretAccessKey" : "xjtest",
            "Token" : "IQote///test",
            "Expiration" : "2021-09-18T03:31:56Z"
        }
        """.trimIndent()
        val deserializer = JsonDeserializer(payload.encodeToByteArray())

        assertFailsWith<InvalidJsonCredentialsException> {
            deserializeJsonCredentials(deserializer)
        }.message.shouldContain("missing field `AccessKeyId`")
    }

    @Test
    fun testMissingSecretAccessKey() = runTest {
        val payload = """
        {
            "LastUpdated" : "2021-09-17T20:57:08Z",
            "Type" : "AWS-HMAC",
            "AccessKeyId" : "ASIARTEST",
            "Token" : "IQote///test",
            "Expiration" : "2021-09-18T03:31:56Z"
        }
        """.trimIndent()
        val deserializer = JsonDeserializer(payload.encodeToByteArray())

        assertFailsWith<InvalidJsonCredentialsException> {
            deserializeJsonCredentials(deserializer)
        }.message.shouldContain("missing field `SecretAccessKey`")
    }

    @Test
    fun testMissingSessionToken() = runTest {
        val payload = """
        {
            "LastUpdated" : "2021-09-17T20:57:08Z",
            "Type" : "AWS-HMAC",
            "AccessKeyId" : "ASIARTEST",
            "SecretAccessKey" : "xjtest",
            "Expiration" : "2021-09-18T03:31:56Z"
        }
        """.trimIndent()
        val deserializer = JsonDeserializer(payload.encodeToByteArray())

        assertFailsWith<InvalidJsonCredentialsException> {
            deserializeJsonCredentials(deserializer)
        }.message.shouldContain("missing field `Token`")
    }

    @Test
    fun testErrorResponse() = runTest {
        val payload = """
        {
            "Code" : "AssumeRoleUnauthorizedAccess",
            "LastUpdated" : "2021-09-17T20:57:08Z",
            "Message": "EC2 cannot assume the role integration-test"
        }
        """.trimIndent()
        val deserializer = JsonDeserializer(payload.encodeToByteArray())

        val parsed = deserializeJsonCredentials(deserializer)
        val expected = JsonCredentialsResponse.Error(
            "AssumeRoleUnauthorizedAccess",
            "EC2 cannot assume the role integration-test",
        )
        assertEquals(expected, parsed)
    }
}

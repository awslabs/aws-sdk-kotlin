/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.auth.credentials.Credentials
import aws.sdk.kotlin.runtime.auth.signing.AwsSignatureType
import aws.sdk.kotlin.runtime.auth.signing.AwsSigningConfig
import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import aws.smithy.kotlin.runtime.io.bytes
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.util.encodeToHex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class EventStreamSigningTest {

    @Test
    fun testSignPayload() = runTest {
        val messageToSign = buildMessage {
            addHeader("some-header", HeaderValue.String("value"))
            payload = "test payload".encodeToByteArray()
        }

        val epoch = Instant.fromEpochSeconds(123_456_789L, 1234)
        val testClock = ManualClock(epoch)
        val signingConfig = AwsSigningConfig.Builder().apply {
            credentials = Credentials("fake access key", "fake secret key")
            region = "us-east-1"
            service = "testservice"
            signatureType = AwsSignatureType.HTTP_REQUEST_CHUNK
        }

        val prevSignature = "last message sts".encodeToByteArray()

        val buffer = SdkByteBuffer(0U)
        messageToSign.encode(buffer)
        val messagePayload = buffer.bytes()
        val result = signPayload(signingConfig, prevSignature, messagePayload, testClock)
        assertEquals(":date", result.output.headers[0].name)

        val dateHeader = result.output.headers[0].value.expectTimestamp()
        assertEquals(epoch.epochSeconds, dateHeader.epochSeconds)
        assertEquals(0, dateHeader.nanosecondsOfSecond)

        assertEquals(":chunk-signature", result.output.headers[1].name)
        val expectedSignature = result.signature.encodeToHex()
        val actualSignature = result.output.headers[1].value.expectByteArray().encodeToHex()
        assertEquals(expectedSignature, actualSignature)
    }
}

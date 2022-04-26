/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.auth.awscredentials.CredentialsProvider
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSignatureType
import aws.smithy.kotlin.runtime.auth.awssigning.AwsSigningConfig
import aws.smithy.kotlin.runtime.auth.awssigning.crt.CrtAwsSigner
import aws.smithy.kotlin.runtime.hashing.sha256
import aws.smithy.kotlin.runtime.io.SdkByteBuffer
import aws.smithy.kotlin.runtime.io.bytes
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.time.ManualClock
import aws.smithy.kotlin.runtime.util.encodeToHex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class EventStreamSigningTest {

    // FIXME - see https://github.com/awslabs/aws-sdk-kotlin/issues/543
    @Ignore
    @Test
    fun testSignPayload() = runTest {
        val messageToSign = buildMessage {
            addHeader("some-header", HeaderValue.String("value"))
            payload = "test payload".encodeToByteArray()
        }

        val epoch = Instant.fromEpochSeconds(123_456_789L, 1234)
        val testClock = ManualClock(epoch)
        val signingConfig = AwsSigningConfig.Builder().apply {
            credentialsProvider = object : CredentialsProvider {
                override suspend fun getCredentials() = Credentials("fake access key", "fake secret key")
            }
            region = "us-east-1"
            service = "testservice"
            signatureType = AwsSignatureType.HTTP_REQUEST_CHUNK
        }

        val prevSignature = "last message sts".encodeToByteArray().sha256().encodeToHex().encodeToByteArray()

        val buffer = SdkByteBuffer(0U)
        messageToSign.encode(buffer)
        val messagePayload = buffer.bytes()
        val result = CrtAwsSigner.signPayload(signingConfig, prevSignature, messagePayload, testClock)
        assertEquals(":date", result.output.headers[0].name)

        val dateHeader = result.output.headers[0].value.expectTimestamp()
        assertEquals(epoch.epochSeconds, dateHeader.epochSeconds)
        assertEquals(0, dateHeader.nanosecondsOfSecond)

        assertEquals(":chunk-signature", result.output.headers[1].name)
        val expectedSignature = result.signature.encodeToHex()
        val actualSignature = result.output.headers[1].value.expectByteArray().encodeToHex()
        assertEquals(expectedSignature, actualSignature)

        // FIXME - based on Rust test: https://github.com/awslabs/smithy-rs/blob/v0.38.0/aws/rust-runtime/aws-sigv4/src/event_stream.rs#L166
        val expected = "1ea04a4f6becd85ae3e38e379ffaf4bb95042603f209512476cc6416868b31ee"
        assertEquals(expected, actualSignature)
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.smithy.kotlin.runtime.auth.awssigning.*
import aws.smithy.kotlin.runtime.client.ExecutionContext
import aws.smithy.kotlin.runtime.io.SdkBuffer
import aws.smithy.kotlin.runtime.time.Clock
import aws.smithy.kotlin.runtime.time.Instant
import aws.smithy.kotlin.runtime.util.InternalApi
import aws.smithy.kotlin.runtime.util.decodeHexBytes
import aws.smithy.kotlin.runtime.util.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Creates a flow that signs each event stream message with the given signing config.
 *
 * Each message's signature incorporates the signature of the previous message.
 * The very first message incorporates the signature of the initial-request for
 * both HTTP2 and WebSockets. The initial signature comes from the execution context.
 */
@InternalApi
public fun Flow<Message>.sign(
    context: ExecutionContext,
    config: AwsSigningConfig,
): Flow<Message> = flow {
    val messages = this@sign

    val signer = context.getOrNull(AwsSigningAttributes.Signer) ?: error("No signer was found in context")

    // NOTE: We need the signature of the initial HTTP request to seed the event stream signatures
    // This is a bit of a chicken and egg problem since the event stream is constructed before the request
    // is signed. The body of the stream shouldn't start being consumed though until after the entire request
    // is built. Thus, by the time we get here the signature will exist in the context.
    var prevSignature = context.getOrNull(AwsSigningAttributes.RequestSignature) ?: error("expected initial HTTP signature to be set before message signing commences")

    // signature date is updated per event message
    val configBuilder = config.toBuilder()

    messages.collect { message ->
        val buffer = SdkBuffer()
        message.encode(buffer)

        // the entire message is wrapped as the payload of the signed message
        val result = signer.signPayload(configBuilder, prevSignature, buffer.readByteArray())
        prevSignature = result.signature
        emit(result.output)
    }

    // end frame - empty body in event stream encoding
    val endFrame = signer.signPayload(configBuilder, prevSignature, ByteArray(0))
    emit(endFrame.output)
}

internal suspend fun AwsSigner.signPayload(
    configBuilder: AwsSigningConfig.Builder,
    prevSignature: ByteArray,
    messagePayload: ByteArray,
    clock: Clock = Clock.System,
): AwsSigningResult<Message> {
    val dt = clock.now().truncateSubsecs()
    val config = configBuilder.apply { signingDate = dt }.build()

    val result = signChunk(messagePayload, prevSignature, config)
    val signature = result.signature
    // TODO - consider adding a direct Bytes -> Bytes hex decode rather than having to go through string
    val binarySignature = signature.decodeToString().decodeHexBytes()

    val signedMessage = buildMessage {
        addHeader(":date", HeaderValue.Timestamp(dt))
        addHeader(":chunk-signature", HeaderValue.ByteArray(binarySignature))
        payload = messagePayload
    }

    return AwsSigningResult(signedMessage, signature)
}

/**
 * Truncate the sub-seconds from the current time
 */
private fun Instant.truncateSubsecs(): Instant = Instant.fromEpochSeconds(epochSeconds, 0)

/**
 * Create a new signing config for an event stream using the current context to set the operation/service specific
 * configuration (e.g. region, signing service, credentials, etc)
 */
@InternalApi
public fun ExecutionContext.newEventStreamSigningConfig(): AwsSigningConfig = AwsSigningConfig {
    algorithm = AwsSigningAlgorithm.SIGV4
    signatureType = AwsSignatureType.HTTP_REQUEST_EVENT
    region = this@newEventStreamSigningConfig[AwsSigningAttributes.SigningRegion]
    service = this@newEventStreamSigningConfig[AwsSigningAttributes.SigningService]
    credentialsProvider = this@newEventStreamSigningConfig[AwsSigningAttributes.CredentialsProvider]
    useDoubleUriEncode = false
    normalizeUriPath = true
    signedBodyHeader = AwsSignedBodyHeader.NONE
}

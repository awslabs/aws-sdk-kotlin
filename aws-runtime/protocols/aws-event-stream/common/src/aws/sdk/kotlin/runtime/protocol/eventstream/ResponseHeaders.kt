/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * Common response headers
 */
public data class ResponseHeaders(
    /**
     * The message type (e.g. `event` or `exception`)
     */
    val messageType: String,

    /**
     * The shape type from the model that the message payload contains
     */
    val shapeType: String,

    /**
     * The payload content type
     */
    val contentType: String? = null
)

@InternalSdkApi
public fun Message.parseResponseHeaders(): ResponseHeaders {
    val headersByName = headers.associateBy { it.name }
    val messageType: String = checkNotNull(headersByName[":message-type"]) { "`:message-type` header is required to deserialize an event stream message" }.value.expectString()
    val eventType = headersByName[":event-type"]?.value?.expectString()
    val exceptionType = headersByName[":exception-type"]?.value?.expectString()
    val contentType = headersByName[":content-type"]?.value?.expectString()

    val shapeType = when (messageType) {
        "event" -> checkNotNull(eventType) { "Invalid `event` message: `:event-type` header is missing" }
        "exception" -> checkNotNull(exceptionType) { "Invalid `exception` message: `:exception-type` header is missing" }
        else -> error("unrecognized `:message-type`: $messageType")
    }

    return ResponseHeaders(messageType, shapeType, contentType)
}

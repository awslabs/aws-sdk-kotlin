/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.runtime.protocol.eventstream

import aws.sdk.kotlin.runtime.InternalSdkApi

/**
 * Parse the protocol level headers into a concrete [MessageType]
 */
@InternalSdkApi
public fun Message.type(): MessageType {
    val headersByName = headers.associateBy { it.name }
    val messageType: String = checkNotNull(headersByName[":message-type"]) { "`:message-type` header is required to deserialize an event stream message" }.value.expectString()
    val eventType = headersByName[":event-type"]?.value?.expectString()
    val exceptionType = headersByName[":exception-type"]?.value?.expectString()
    val contentType = headersByName[":content-type"]?.value?.expectString()

    return when (messageType) {
        "event" -> MessageType.Event(
            checkNotNull(eventType) { "Invalid `event` message: `:event-type` header is missing" },
            contentType
        )
        "exception" -> MessageType.Exception(
            checkNotNull(exceptionType) { "Invalid `exception` message: `:exception-type` header is missing" },
            contentType
        )
        "error" -> {
            val errorCode = headersByName[":error-code"]?.value?.expectString() ?: error("Invalid `error` message: `:error-code` header is missing")
            val errorMessage = headersByName[":error-message"]?.value?.expectString()
            MessageType.Error(errorCode, errorMessage)
        }

        else -> MessageType.SdkUnknown(messageType)
    }
}

/**
 * Common framework message information parsed from headers
 */
@InternalSdkApi
public sealed class MessageType {
    /**
     * Corresponds to the `event` message type. All events include the headers:
     *
     * * `:message-type`: Always set to `event`
     * * `:event-type`: (Required) Identifies the event shape from the event stream union. This is the member name from the union.
     * * `:content-type`: (Optional) The content type for the payload
     *
     * ### Example message
     *
     * ```
     * :message-type: event
     * :event-type: MyStruct
     * :content-type: application/json
     *
     * {...}
     * ```
     * @param shapeType the event type as identified by the `:event-type` header.
     * @param contentType the content type of the payload (if present)
     */
    public data class Event(val shapeType: String, val contentType: String? = null) : MessageType()

    /**
     * Corresponds to the `exception` message type.
     * NOTE: Exceptions are mapped directly to the payload. There is no way to map event headers for exceptions.
     *
     * ### Example message
     *
     * ```
     * :message-type: exception
     * :exception-type: FooException
     * :content-type: application/json
     *
     * {...}
     * ```
     *
     * @param shapeType the exception type as identified by the `:exception-type` header.
     * @param contentType the content type of the payload (if present)
     */
    public data class Exception(val shapeType: String, val contentType: String? = null) : MessageType()

    /**
     * Corresponds to the `error` message type.
     * Errors are like exceptions, but they are un-modeled and have a fixed set of fields:
     * * `:message-type`: Always set to `error`
     * * `:error-code`: (Required) UTF-8 string containing name, type, or category of the error.
     * * `:error-message`: (Optional) UTF-* string containing an error message
     *
     * ### Example message
     *
     * ```
     * :message-type: error
     * :error-code: InternalServerError
     * :error-message: An error occurred
     * ```
     */
    public data class Error(val errorCode: String, val message: String? = null) : MessageType()

    /**
     * Catch all for unknown message types outside of `event`, `exception`, or `error`
     */
    public data class SdkUnknown(val messageType: String) : MessageType()
}

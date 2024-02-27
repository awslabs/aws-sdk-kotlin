/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.services.route53.internal

import aws.sdk.kotlin.services.route53.model.InvalidChangeBatch
import aws.smithy.kotlin.runtime.awsprotocol.ErrorDetails
import aws.smithy.kotlin.runtime.serde.xml.*

internal fun parseRestXmlInvalidChangeBatchResponse(payload: ByteArray): InvalidChangeBatchErrorResponse? =
    deserializeInvalidChangeBatchError(InvalidChangeBatch.Builder(), payload)

internal fun deserializeInvalidChangeBatchError(builder: InvalidChangeBatch.Builder, payload: ByteArray): InvalidChangeBatchErrorResponse? {
    val root = xmlTagReader(payload)
    var requestId: String? = null

    loop@while (true) {
        val curr = root.nextTag() ?: break@loop
        when (curr.tagName) {
            "Message", "message" -> builder.message = curr.data()
            "Messages", "messages" -> builder.messages = deserializeMessages(curr)
            "RequestId" -> requestId = curr.data()
        }
        curr.drop()
    }

    return InvalidChangeBatchErrorResponse(ErrorDetails("InvalidChangeBatch", builder.message, requestId), builder.build())
}

private fun deserializeMessages(root: XmlTagReader): List<String> {
    val result = mutableListOf<String>()
    loop@while (true) {
        val curr = root.nextTag() ?: break@loop
        when (curr.tagName) {
            "Message" -> {
                val el = curr.tryData().getOrNull() ?: continue@loop
                result.add(el)
            }
        }
    }

    return result
}

internal data class InvalidChangeBatchErrorResponse(
    val errorDetails: ErrorDetails,
    val exception: InvalidChangeBatch,
)

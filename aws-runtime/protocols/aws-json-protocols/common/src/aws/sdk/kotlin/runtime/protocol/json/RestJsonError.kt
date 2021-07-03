/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.json

import aws.sdk.kotlin.runtime.*
import aws.sdk.kotlin.runtime.http.*
import aws.sdk.kotlin.runtime.http.middleware.errors.AbstractErrorHandling
import aws.smithy.kotlin.runtime.http.*

/**
 * Http feature that inspects responses and throws the appropriate modeled service error that matches
 *
 * @property registry Modeled exceptions registered with the feature. All responses will be inspected to
 * see if one of the registered errors matches
 */
@InternalSdkApi
public class RestJsonError(registry: ExceptionRegistry) : AbstractErrorHandling(registry) {
    public companion object Feature : AbstractFeature<RestJsonError>() {
        override val key: FeatureKey<RestJsonError> = FeatureKey("RestJsonError")
        override fun create(config: Config) = RestJsonError(config.registry)
    }

    protected override val protocolName = "restJson"
    override suspend fun parseErrorResponse(headers: Headers, payload: ByteArray?) =
        RestJsonErrorDeserializer.deserialize(headers, payload)
}

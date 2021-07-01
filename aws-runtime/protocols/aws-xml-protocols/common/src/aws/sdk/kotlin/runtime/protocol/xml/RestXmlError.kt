/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.xml

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.http.ExceptionRegistry
import aws.smithy.kotlin.runtime.http.FeatureKey

/**
 * Http feature that inspects responses and throws the appropriate modeled service error that matches
 *
 * @property registry Modeled exceptions registered with the feature. All responses will be inspected to
 * see if one of the registered errors matches
 */
@InternalSdkApi
public class RestXmlError(registry: ExceptionRegistry) : GenericXmlErrorHandling(registry) {
    public companion object Feature : GenericFeature<RestXmlError>() {
        override val key: FeatureKey<RestXmlError> = FeatureKey("Ec2QueryErrorHandling")
        override fun create(config: Config) = RestXmlError(config.registry)
    }

    override suspend fun parseErrorResponse(payload: ByteArray) = parseRestXmlErrorResponse(payload)
}

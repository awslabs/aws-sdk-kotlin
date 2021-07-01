/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.runtime.protocol.xml

import aws.sdk.kotlin.runtime.InternalSdkApi
import aws.sdk.kotlin.runtime.http.ExceptionRegistry
import aws.smithy.kotlin.runtime.http.FeatureKey

/**
 * Http feature that inspects responses and throws the appropriate modeled service error.
 *
 * @property registry Modeled exceptions registered with the feature. All responses will be inspected to see if one of
 * the registered errors matches.
 */
@InternalSdkApi
public class Ec2QueryErrorHandling(registry: ExceptionRegistry) : GenericXmlErrorHandling(registry) {
    public companion object Feature : GenericFeature<Ec2QueryErrorHandling>() {
        override val key: FeatureKey<Ec2QueryErrorHandling> = FeatureKey("Ec2QueryErrorHandling")
        override fun create(config: Config) = Ec2QueryErrorHandling(config.registry)
    }

    override suspend fun parseErrorResponse(payload: ByteArray) = parseEc2QueryErrorResponse(payload)
}

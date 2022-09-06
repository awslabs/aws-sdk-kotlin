/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.json

import aws.sdk.kotlin.codegen.protocols.core.StaticHttpBindingResolver
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.pattern.UriPattern
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.*

/**
 * An HTTP binding resolver for the awsJson protocol(s).
 */
class AwsJsonHttpBindingResolver(
    model: Model,
    serviceShape: ServiceShape,
    defaultContentType: String,
) : StaticHttpBindingResolver(model, serviceShape, AwsJsonHttpTrait, defaultContentType, TimestampFormatTrait.Format.EPOCH_SECONDS) {
    companion object {
        val AwsJsonHttpTrait: HttpTrait = HttpTrait
            .builder()
            .code(200)
            .method("POST")
            .uri(UriPattern.parse("/"))
            .build()
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.protocols.middleware.ModeledExceptionsMiddleware
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.addImport
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator

/**
 * Middleware to handle custom S3 error messages
 */
class S3ErrorMiddleware(
    ctx: ProtocolGenerator.GenerationContext,
    httpBindingResolver: HttpBindingResolver
) : ModeledExceptionsMiddleware(ctx, httpBindingResolver) {
    override val name: String = "S3ErrorFeature"

    override fun addImportsAndDependencies(writer: KotlinWriter) {
        super.addImportsAndDependencies(writer)
        writer.addImport("aws.sdk.kotlin.service.s3.internal", "S3ErrorFeature")
    }
}

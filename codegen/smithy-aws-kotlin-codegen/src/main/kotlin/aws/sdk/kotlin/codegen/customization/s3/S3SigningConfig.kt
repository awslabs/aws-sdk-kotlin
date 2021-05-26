/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.middleware.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Overrides the SigV4 signing middleware config for S3.
 */
class S3SigningConfig : KotlinIntegration {

    override val order: Byte
        get() = 127

    override fun apply(service: ServiceShape) = service.isS3

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        generator: HttpBindingProtocolGenerator,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> {
        val signingServiceName = AwsSignatureVersion4.signingServiceName(ctx.model, ctx.service)

        return resolved.replace(newValue = S3SigningMiddleware(signingServiceName)) { middleware ->
            middleware.name == AwsRuntimeTypes.Auth.AwsSigV4SigningMiddleware.name
        }
    }
}

private class S3SigningMiddleware(signingServiceName: String) : AwsSignatureVersion4(signingServiceName) {
    override fun renderConfigure(writer: KotlinWriter) {
        super.renderConfigure(writer)
        val sbht = AwsRuntimeTypes.Auth.AwsSignedBodyHeaderType
        writer.addImport(sbht)
        writer.write("signedBodyHeaderType = #T.X_AMZ_CONTENT_SHA256", sbht)
    }
}

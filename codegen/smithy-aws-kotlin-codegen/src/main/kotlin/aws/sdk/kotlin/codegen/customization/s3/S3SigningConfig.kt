/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.customization.s3

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.middleware.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.integration.ProtocolMiddleware

/**
 * Overrides the SigV4 signing middleware config for S3.
 */
class S3SigningConfig : KotlinIntegration {

    override val order: Byte
        get() = 127

    override fun customizeMiddleware(
        ctx: ProtocolGenerator.GenerationContext,
        resolved: List<ProtocolMiddleware>
    ): List<ProtocolMiddleware> {
        if (!ctx.service.isS3) return resolved

        val middleware = resolved.filterNot {
            it.name == AwsRuntimeTypes.Auth.AwsSigV4SigningMiddleware.name
        }.toMutableList()

        val signingServiceName = AwsSignatureVersion4.signingServiceName(ctx.model, ctx.service)
        middleware.add(S3SigningMiddleware(signingServiceName))

        return middleware
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

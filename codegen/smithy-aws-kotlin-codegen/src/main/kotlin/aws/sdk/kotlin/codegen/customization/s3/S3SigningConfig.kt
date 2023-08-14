/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.customization.s3

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.AuthSchemeHandler
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.rendering.auth.SigV4AuthSchemeHandler
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Overrides the SigV4 auth scheme registered by [software.amazon.smithy.kotlin.codegen.rendering.auth.Sigv4AuthSchemeIntegration] for S3.
 */
class S3SigningConfig : KotlinIntegration {
    // auth schemes are de-duped by taking the last one registered
    override val order: Byte
        get() = 127

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun authSchemes(ctx: ProtocolGenerator.GenerationContext): List<AuthSchemeHandler> =
        listOf(S3AuthSchemeHandler())
}

private class S3AuthSchemeHandler : SigV4AuthSchemeHandler() {
    override fun instantiateAuthSchemeExpr(ctx: ProtocolGenerator.GenerationContext, writer: KotlinWriter) {
        val signingService = AwsSignatureVersion4.signingServiceName(ctx.service)
        writer.withBlock("#T(", ")", RuntimeTypes.Auth.HttpAuthAws.SigV4AuthScheme) {
            withBlock("#T.Config().apply {", "}", RuntimeTypes.Auth.HttpAuthAws.AwsHttpSigner) {
                write("signer = #T", RuntimeTypes.Auth.Signing.AwsSigningStandard.DefaultAwsSigner)
                write("service = #S", signingService)
                write("signedBodyHeader = #T.X_AMZ_CONTENT_SHA256", RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSignedBodyHeader)
                // https://github.com/awslabs/aws-sdk-kotlin/issues/200
                writer.write("useDoubleUriEncode = false")
                writer.write("normalizeUriPath = false")
            }
        }
    }
}

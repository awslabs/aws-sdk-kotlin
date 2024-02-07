/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3.express

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.customization.s3.ClientConfigIntegration
import aws.sdk.kotlin.codegen.customization.s3.isS3
import software.amazon.smithy.aws.traits.HttpChecksumTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.AppendingSectionWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.auth.AuthSchemeProviderGenerator
import software.amazon.smithy.kotlin.codegen.rendering.auth.IdentityProviderConfigGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait
import software.amazon.smithy.model.traits.HttpHeaderTrait

/**
 * An integration which handles codegen for S3 Express, such as:
 * 1. Configure auth scheme (auth scheme ID, auth option, identity provider for auth scheme)
 * 2. Add S3Client and Bucket to execution context (required for the S3 Express credentials cache key)
 * 3. Override checksums to use CRC32 instead of MD5
 * 4. Disable all checksums for s3:UploadPart
 */
class S3ExpressIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) = model.expectShape<ServiceShape>(settings.service).isS3

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(HttpProtocolClientGenerator.ConfigureAuthSchemes, configureS3ExpressAuthSchemeWriter),
            SectionWriterBinding(AuthSchemeProviderGenerator.ServiceDefaults, setServiceDefaultAuthOptionWriter),
            SectionWriterBinding(IdentityProviderConfigGenerator.ConfigureIdentityProviderForAuthScheme, configureIdentityProviderForAuthSchemeWriter),
        )

    private val configureS3ExpressAuthSchemeWriter = AppendingSectionWriter { writer ->
        writer.withBlock("getOrPut(#T) {", "}", SigV4S3ExpressAuthSchemeHandler().authSchemeIdSymbol) {
            writer.write("#T(#T, #S)", AwsRuntimeTypes.Config.Auth.SigV4S3ExpressAuthScheme, RuntimeTypes.Auth.Signing.AwsSigningStandard.DefaultAwsSigner, "s3")
        }
    }

    private val setServiceDefaultAuthOptionWriter = AppendingSectionWriter { writer ->
        writer.write("#T(),", AwsRuntimeTypes.Config.Auth.sigV4S3Express)
    }

    private val configureIdentityProviderForAuthSchemeWriter = AppendingSectionWriter { writer ->
        writer.write("#S -> config.#L", "aws.auth#sigv4s3express", ClientConfigIntegration.S3ExpressCredentialsProvider.propertyName)
    }

    override fun customizeMiddleware(ctx: ProtocolGenerator.GenerationContext, resolved: List<ProtocolMiddleware>) =
        resolved + listOf(
        AddClientToExecutionContext,
        AddBucketToExecutionContext,
        UseCrc32Checksum,
        UploadPartDisableChecksum,
    )

    private val AddClientToExecutionContext = object : ProtocolMiddleware {
        override val name: String = "AddClientToExecutionContext"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = ctx.model.expectShape<ServiceShape>(ctx.settings.service).isS3

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val attributesSymbol = AwsRuntimeTypes.Config.Auth.S3ExpressAttributes
            writer.write("op.context[#T.Client] = this", attributesSymbol)
        }
    }

    private val AddBucketToExecutionContext = object : ProtocolMiddleware {
        override val name: String = "AddBucketToExecutionContext"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            ctx.model.expectShape<StructureShape>(op.input.get())
                .members()
                .any { it.memberName == "Bucket" }

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val attributesSymbol = AwsRuntimeTypes.Config.Auth.S3ExpressAttributes
            writer.write("input.bucket?.let { op.context[#T.Bucket] = it }", attributesSymbol)
        }
    }

    /**
     * For any operations that may send a checksum, override a user-configured checksum to set CRC32.
     */
    private val UseCrc32Checksum = object : ProtocolMiddleware {
        override val name: String = "UseCrc32Checksum"

        override val order: Byte = -1 // Render before flexible checksums

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            !op.isS3UploadPart && (op.hasTrait<HttpChecksumRequiredTrait>() || op.hasTrait<HttpChecksumTrait>())

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()

            val checksumAlgorithmMember = ctx.model.expectShape<StructureShape>(op.input.get())
                .members()
                .firstOrNull { it.memberName == httpChecksumTrait?.requestAlgorithmMember?.getOrNull() }

            // S3 models a header name x-amz-sdk-checksum-algorithm representing the name of the checksum algorithm used
            val checksumHeaderName = checksumAlgorithmMember?.getTrait<HttpHeaderTrait>()?.value

            val checksumRequired = op.hasTrait<HttpChecksumRequiredTrait>() || httpChecksumTrait?.isRequestChecksumRequired == true

            if (checksumRequired) {
                if (checksumAlgorithmMember != null) { // enable flexible checksums using CRC32 if the user has not already opted-in
                    writer.withBlock("if (input.${checksumAlgorithmMember.defaultName()} == null) {", "}") {
                        writer.write("op.interceptors.add(#T(${checksumHeaderName?.dq()}))", AwsRuntimeTypes.Http.Interceptors.S3ExpressCrc32ChecksumInterceptor)
                    }
                } else { // checksum required, operation does not expose flexible checksums member, so set the checksum algorithm manually
                    writer.write("op.interceptors.add(#T())", AwsRuntimeTypes.Http.Interceptors.S3ExpressCrc32ChecksumInterceptor)
                }
            }
        }
    }

    private val UploadPartDisableChecksum = object : ProtocolMiddleware {
        override val name: String = "UploadPartDisableChecksum"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean = op.isS3UploadPart

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val interceptorSymbol = AwsRuntimeTypes.Http.Interceptors.S3ExpressDisableChecksumInterceptor
            writer.addImport(interceptorSymbol)
            writer.write("op.interceptors.add(#T())", interceptorSymbol)
        }
    }

    private val OperationShape.isS3UploadPart: Boolean get() = id.name == "UploadPart"
}

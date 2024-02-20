/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3.express

import SigV4S3ExpressAuthTrait
import aws.sdk.kotlin.codegen.customization.s3.isS3
import software.amazon.smithy.aws.traits.HttpChecksumTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.utils.dq
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.AuthTrait
import software.amazon.smithy.model.traits.HttpChecksumRequiredTrait
import software.amazon.smithy.model.traits.HttpHeaderTrait
import software.amazon.smithy.model.transform.ModelTransformer

/**
 * An integration which handles codegen for S3 Express, such as:
 * 1. Configure auth scheme (auth scheme ID, auth option, identity provider for auth scheme)
 * 2. Add S3Client and Bucket to execution context (required for the S3 Express credentials cache key)
 * 3. Override checksums to use CRC32 instead of MD5
 * 4. Disable all checksums for s3:UploadPart
 */
class S3ExpressIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).isS3

    /**
     * Add a synthetic SigV4 S3 Express auth trait
     */
    override fun preprocessModel(model: Model, settings: KotlinSettings): Model =
        ModelTransformer.create().mapShapes(model) { shape ->
            when {
                shape.isServiceShape -> {
                    val builder = (shape as ServiceShape).toBuilder()

                    builder.addTrait(SigV4S3ExpressAuthTrait())

                    val authTrait =
                        AuthTrait(mutableSetOf(SigV4S3ExpressAuthTrait.ID) + shape.expectTrait(AuthTrait::class.java).valueSet)
                    builder.addTrait(authTrait)

                    builder.build()
                }

                else -> shape
            }
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

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            ctx.model.expectShape<ServiceShape>(ctx.settings.service).isS3

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val attributesSymbol = buildSymbol {
                name = "S3Attributes"
                namespace = "aws.sdk.kotlin.services.s3"
            }
            writer.write("op.context[#T.ExpressClient] = this", attributesSymbol)
        }
    }

    private val AddBucketToExecutionContext = object : ProtocolMiddleware {
        override val name: String = "AddBucketToExecutionContext"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            ctx.model.expectShape<StructureShape>(op.input.get())
                .members()
                .any { it.memberName == "Bucket" }

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val attributesSymbol = buildSymbol {
                name = "S3Attributes"
                namespace = "aws.sdk.kotlin.services.s3"
            }
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
            val interceptorSymbol = buildSymbol {
                namespace = "aws.sdk.kotlin.services.s3.express"
                name = "S3ExpressCrc32ChecksumInterceptor"
            }

            val httpChecksumTrait = op.getTrait<HttpChecksumTrait>()

            val checksumAlgorithmMember = ctx.model.expectShape<StructureShape>(op.input.get())
                .members()
                .firstOrNull { it.memberName == httpChecksumTrait?.requestAlgorithmMember?.getOrNull() }

            // S3 models a header name x-amz-sdk-checksum-algorithm representing the name of the checksum algorithm used
            val checksumHeaderName = checksumAlgorithmMember?.getTrait<HttpHeaderTrait>()?.value

            val checksumRequired =
                op.hasTrait<HttpChecksumRequiredTrait>() || httpChecksumTrait?.isRequestChecksumRequired == true

            if (checksumRequired) {
                writer.write("op.interceptors.add(#T(${checksumHeaderName?.dq() ?: ""}))", interceptorSymbol)
            }
        }
    }

    private val UploadPartDisableChecksum = object : ProtocolMiddleware {
        override val name: String = "UploadPartDisableChecksum"

        override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean =
            op.isS3UploadPart

        override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
            val interceptorSymbol = buildSymbol {
                namespace = "aws.sdk.kotlin.services.s3.express"
                name = "S3ExpressDisableChecksumInterceptor"
            }
            writer.addImport(interceptorSymbol)
            writer.write("op.interceptors.add(#T())", interceptorSymbol)
        }
    }

    private val OperationShape.isS3UploadPart: Boolean get() = id.name == "UploadPart"
}

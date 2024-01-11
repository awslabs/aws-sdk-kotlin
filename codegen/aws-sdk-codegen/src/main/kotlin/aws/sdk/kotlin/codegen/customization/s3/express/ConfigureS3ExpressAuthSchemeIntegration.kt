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
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes.Auth.Identity.AuthSchemeId
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.AppendingSectionWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.getTrait
import software.amazon.smithy.kotlin.codegen.rendering.auth.AuthSchemeProviderGenerator
import software.amazon.smithy.kotlin.codegen.rendering.auth.IdentityProviderConfigGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.kotlin.codegen.utils.getOrNull
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape

class ConfigureS3ExpressAuthSchemeIntegration : KotlinIntegration {
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
        resolved + AddClientToExecutionContext + AddBucketToExecutionContext

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

}
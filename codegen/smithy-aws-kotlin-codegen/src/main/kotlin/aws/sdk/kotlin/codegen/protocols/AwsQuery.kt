/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols

import aws.sdk.kotlin.codegen.protocols.core.AwsHttpBindingProtocolGenerator
import aws.sdk.kotlin.codegen.protocols.core.AwsHttpProtocolClientGenerator
import aws.sdk.kotlin.codegen.protocols.core.StaticHttpBindingResolver
import aws.sdk.kotlin.codegen.protocols.middleware.MutateHeadersMiddleware
import aws.sdk.kotlin.codegen.protocols.query.QuerySerdeProviderGenerator
import aws.sdk.kotlin.codegen.protocols.xml.RestXmlErrorMiddleware
import software.amazon.smithy.aws.traits.protocols.AwsQueryTrait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.rendering.protocol.*
import software.amazon.smithy.kotlin.codegen.rendering.serde.FormUrlSerdeDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.SerdeDescriptorGenerator
import software.amazon.smithy.kotlin.codegen.rendering.serde.SerdeTargetUse
import software.amazon.smithy.kotlin.codegen.rendering.serde.XmlSerdeDescriptorGenerator
import software.amazon.smithy.model.pattern.UriPattern
import software.amazon.smithy.model.shapes.*
import software.amazon.smithy.model.traits.*

private const val AwsQueryContentType: String = "application/x-www-form-urlencoded"

/**
 * Handles generating the aws.protocols#awsQuery protocol for services.
 *
 * @inheritDoc
 * @see AwsHttpBindingProtocolGenerator
 */
class AwsQuery : AwsHttpBindingProtocolGenerator() {
    override val protocol: ShapeId = AwsQueryTrait.ID

    override val defaultTimestampFormat: TimestampFormatTrait.Format = TimestampFormatTrait.Format.DATE_TIME

    override fun getProtocolHttpBindingResolver(ctx: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        AwsQueryBindingResolver(ctx)

    override fun getHttpProtocolClientGenerator(ctx: ProtocolGenerator.GenerationContext): HttpProtocolClientGenerator {
        val middleware = getHttpMiddleware(ctx)
        return AwsQueryProtocolClientGenerator(ctx, middleware, getProtocolHttpBindingResolver(ctx))
    }

    override fun getDefaultHttpMiddleware(ctx: ProtocolGenerator.GenerationContext): List<ProtocolMiddleware> {
        val middleware = super.getDefaultHttpMiddleware(ctx)

        val awsQueryMiddleware = listOf(
            RestXmlErrorMiddleware(ctx, getProtocolHttpBindingResolver(ctx)),
            // ensure content-type gets set
            // see: https://awslabs.github.io/smithy/1.0/spec/aws/aws-query-protocol.html#protocol-behavior
            MutateHeadersMiddleware(addMissingHeaders = mapOf("Content-Type" to AwsQueryContentType))
        )

        return middleware + awsQueryMiddleware
    }

    override fun getSerdeDescriptorGenerator(
        ctx: ProtocolGenerator.GenerationContext,
        objectShape: Shape,
        members: List<MemberShape>,
        targetUse: SerdeTargetUse,
        writer: KotlinWriter
    ): SerdeDescriptorGenerator {
        val renderingCtx = ctx.toRenderingContext(this, objectShape, writer)
        return if (targetUse.isSerializer) {
            FormUrlSerdeDescriptorGenerator(renderingCtx, members)
        }else {
            XmlSerdeDescriptorGenerator(renderingCtx, members)
        }
    }
}

private class AwsQueryBindingResolver(
    context: ProtocolGenerator.GenerationContext,
) : StaticHttpBindingResolver(context, AwsQueryHttpTrait, AwsQueryContentType, TimestampFormatTrait.Format.DATE_TIME) {
    companion object {
        val AwsQueryHttpTrait: HttpTrait = HttpTrait
            .builder()
            .code(200)
            .method("POST")
            .uri(UriPattern.parse("/"))
            .build()
    }
}


private class AwsQueryProtocolClientGenerator(
    ctx: ProtocolGenerator.GenerationContext,
    middlewares: List<ProtocolMiddleware>,
    httpBindingResolver: HttpBindingResolver
) : AwsHttpProtocolClientGenerator(ctx, middlewares, httpBindingResolver) {

    override val serdeProviderSymbol: Symbol
        get() = buildSymbol {
            name = "AwsQuerySerdeProvider"
            namespace = "${ctx.settings.pkg.name}.internal"
            definitionFile = "${name}.kt"
        }

    override fun render(writer: KotlinWriter) {
        super.render(writer)

        // render the serde provider symbol to internals package
        ctx.delegator.useShapeWriter(serdeProviderSymbol) {
            QuerySerdeProviderGenerator(serdeProviderSymbol).render(it)
        }
    }
}

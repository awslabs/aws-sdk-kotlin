/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.polly

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.PresignerGenerator
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriter
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpStringValuesMapSerializer
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Override the PresignedRequestConfig instance generation for Polly based on customization SEP
 */
class PollyPresigner : KotlinIntegration {

    override fun enabledForService(model: Model, settings: KotlinSettings) =
        model.expectShape<ServiceShape>(settings.service).id.toString() == "com.amazonaws.polly#Parrot_v1"

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(SectionWriterBinding(PresignerGenerator.PresignConfigFnSection, addPollyPresignConfigFnWriter))

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        delegator.runtimeDependencies.addAll(KotlinDependency.KOTLIN_TEST.dependencies)
        delegator.runtimeDependencies.addAll(AwsKotlinDependency.AWS_TESTING.dependencies)
    }

    private val addPollyPresignConfigFnWriter = SectionWriter { writer, _ ->
        val ctx = writer.getContextValue(PresignerGenerator.PresignConfigFnSection.CodegenContext)
        val operation = ctx.model.expectShape<OperationShape>(writer.getContextValue(PresignerGenerator.PresignConfigFnSection.OperationId))
        val resolver = writer.getContextValue(PresignerGenerator.PresignConfigFnSection.HttpBindingResolver)
        val defaultTimestampFormat = writer.getContextValue(PresignerGenerator.PresignConfigFnSection.DefaultTimestampFormat)

        writer.addImport(RuntimeTypes.Core.Net.QueryParametersBuilder)
        writer.addImport(RuntimeTypes.Http.HttpMethod)
        writer.write(
            """            
            require(duration.isPositive()) { "duration must be greater than zero" }
            val httpRequestBuilder = SynthesizeSpeechOperationSerializer().serialize(ExecutionContext.build { }, input)
            val queryStringBuilder = QueryParametersBuilder()
            """.trimIndent(),
        )

        writer.openBlock("with(queryStringBuilder) {", "}") {
            val bindings = resolver.requestBindings(operation)
            HttpStringValuesMapSerializer(ctx.model, ctx.symbolProvider, bindings, resolver, defaultTimestampFormat).render(writer)
        }

        writer.write(
            """
            return #T(
                HttpMethod.GET,
                httpRequestBuilder.url.path,
                queryStringBuilder.build(),
                duration,
                true,
                #T.QUERY_STRING,
            )
            """.trimIndent(),
            RuntimeTypes.Auth.Signing.AwsSigningCommon.PresignedRequestConfig,
            RuntimeTypes.Auth.Signing.AwsSigningCommon.PresigningLocation,
        )
    }
}

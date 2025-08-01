/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.SdkIdTransform
import aws.sdk.kotlin.codegen.ServiceClientCompanionObjectWriter
import aws.sdk.kotlin.codegen.withTransform
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.AppendingSectionWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.rendering.ServiceClientGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.HttpBearerAuthTrait

/**
 * Customization that enables sourcing Bearer tokens from an environment variable
 *
 * When a service-specific environment variable for bearer tokens is present (e.g., AWS_BEARER_TOKEN_BEDROCK),
 * this customization configures the auth scheme resolver to prefer the smithy.api#httpBearerAuth scheme
 * over other authentication methods. Additionally, it configures a token provider that extracts the bearer token
 * from the target environment variable.
 */
class EnvironmentBearerTokenCustomization : KotlinIntegration {
    // Currently only services with sigv4 service name 'bedrock' need this customization
    private val supportedSigningServiceNames = setOf("bedrock")

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val serviceShape = settings.getService(model)
        if (!AwsSignatureVersion4.isSupportedAuthentication(model, serviceShape)) {
            return false
        }
        if (!serviceShape.hasTrait<HttpBearerAuthTrait>()) {
            return false
        }

        return AwsSignatureVersion4.signingServiceName(serviceShape) in supportedSigningServiceNames
    }

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val serviceShape = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val packageName = ctx.settings.pkg.name

        delegator.useFileWriter(
            "FinalizeBearerTokenConfig.kt",
            "$packageName.auth",
        ) { writer ->
            renderEnvironmentBearerTokenConfig(
                writer,
                ctx,
                serviceShape,
            )
        }
    }

    private fun renderEnvironmentBearerTokenConfig(
        writer: KotlinWriter,
        ctx: CodegenContext,
        serviceShape: ServiceShape,
    ) {
        val serviceSymbol = ctx.symbolProvider.toSymbol(serviceShape)
        val signingServiceName = AwsSignatureVersion4.signingServiceName(serviceShape)
        // Transform signing name to environment variable name
        val envVarSuffix = signingServiceName.withTransform(SdkIdTransform.UpperSnakeCase)
        val envVarName = "AWS_BEARER_TOKEN_$envVarSuffix"
        val authSchemeId = RuntimeTypes.Auth.Identity.AuthSchemeId

        writer.withBlock(
            "internal fun finalizeBearerTokenConfig(builder: #1T.Builder, provider: #2T = #2T.System) {",
            "}",
            serviceSymbol,
            RuntimeTypes.Core.Utils.PlatformProvider,
        ) {
            // The customization do nothing if environment variable is not set
            write("if (provider.getenv(#S) == null) { return }", envVarName)

            // Configure auth scheme preference if customer hasn't specify one
            write("builder.config.authSchemePreference = builder.config.authSchemePreference ?: listOf(#T.HttpBearer)", authSchemeId)

            // Promote HttpBearer to first position in auth scheme preference list
            withBlock("val filteredSchemes = builder.config.authSchemePreference?.filterNot {", "} ?: emptyList()") {
                write("it == #T.HttpBearer", authSchemeId)
            }

            write("builder.config.authSchemePreference = listOf(#1T.HttpBearer) + filteredSchemes", authSchemeId)

            write(
                "builder.config.bearerTokenProvider = builder.config.bearerTokenProvider ?: #T(#S, provider)",
                RuntimeTypes.Auth.HttpAuth.EnvironmentBearerTokenProvider,
                envVarName,
            )
        }
    }

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(
                ServiceClientCompanionObjectWriter.FinalizeEnvironmentalConfig,
                finalizeEnvironmentBearerTokenConfigWriter,
            ),
        )

    private val finalizeEnvironmentBearerTokenConfigWriter = AppendingSectionWriter { writer ->
        val serviceName = clientName(writer.getContextValue(ServiceClientGenerator.Sections.CompanionObject.SdkId))

        val environmentBearerTokenConfig = buildSymbol {
            name = "finalizeBearerTokenConfig"
            namespace = "aws.sdk.kotlin.services.${serviceName.lowercase()}.auth"
        }

        writer.write("#T(builder)", environmentBearerTokenConfig)
    }
}

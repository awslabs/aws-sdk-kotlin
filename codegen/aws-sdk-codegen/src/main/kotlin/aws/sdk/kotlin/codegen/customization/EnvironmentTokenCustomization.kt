/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.ServiceClientCompanionObjectWriter
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.*
import software.amazon.smithy.kotlin.codegen.integration.AppendingSectionWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.rendering.ServiceClientGenerator
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape

/**
 * Customization that support sourcing Bearer tokens from an environment variable
 *
 * When a service-specific environment variable for bearer tokens is present (e.g., AWS_BEARER_TOKEN_BEDROCK),
 * this customization configures the auth scheme resolver to prefer the smithy.api#httpBearerAuth scheme
 * over other authentication methods. Additionally, it configures a token provider that extracts the bearer token
 * from the target environment variable.
 */
class EnvironmentTokenCustomization : KotlinIntegration {
    // Currently only services with sigv4 service name 'bedrock' need this customization
    private val supportedSigningServiceNames = setOf("bedrock")

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val serviceShape = settings.getService(model)
        if (!AwsSignatureVersion4.isSupportedAuthentication(model, serviceShape)) {
            return false
        }
        val signingServiceName = AwsSignatureVersion4.signingServiceName(serviceShape)

        return signingServiceName in supportedSigningServiceNames
    }

    override fun writeAdditionalFiles(ctx: CodegenContext, delegator: KotlinDelegator) {
        val serviceShape = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val serviceName = ctx.symbolProvider.toSymbol(serviceShape).name.removeSuffix("Client")
        val packageName = ctx.settings.pkg.name

        delegator.useFileWriter(
            "Finalize${serviceName}EnvironmentTokenConfig.kt",
            "$packageName.auth",
        ) { writer ->
            renderEnvironmentTokenConfig(
                writer,
                ctx,
            )
        }
    }

    private fun renderEnvironmentTokenConfig(
        writer: KotlinWriter,
        ctx: CodegenContext,
    ) {
        val serviceShape = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        val serviceSymbol = ctx.symbolProvider.toSymbol(serviceShape)
        val serviceName = serviceSymbol.name.removeSuffix("Client")
        val signingName = AwsSignatureVersion4.signingServiceName(serviceShape)
        // Transform signing name to environment variable name
        val envVarName = "AWS_BEARER_TOKEN_" + signingName.replace("""[-\s]""".toRegex(), "_").uppercase()

        writer.apply {
            openBlock(
                "internal fun finalize#LEnvironmentTokenConfig(",
                serviceName,
            )
            write(
                "builder: #T.Builder,",
                serviceSymbol,
            )
            write(
                "provider: #T = #T.System",
                RuntimeTypes.Core.Utils.PlatformProvider,
                RuntimeTypes.Core.Utils.PlatformProvider,
            )

            closeAndOpenBlock(") {")

            // The customization do nothing if environment variable is not set
            openBlock(
                "if (provider.getenv(#S) != null) {",
                envVarName,
            )

            // Configure auth scheme preference if customer hasn't specify one
            write(
                "builder.config.authSchemePreference = builder.config.authSchemePreference ?: listOf(#T.HttpBearer)",
                RuntimeTypes.Auth.Identity.AuthSchemeId,
            )

            // Promote HttpBearer to first position in auth scheme preference list
            openBlock("val filteredSchemes = builder.config.authSchemePreference?.filterNot {")

            write(
                "it == #T.HttpBearer",
                RuntimeTypes.Auth.Identity.AuthSchemeId,
            )

            closeBlock("}?: emptyList()")

            write(
                "builder.config.authSchemePreference = (listOf(#T.HttpBearer) + filteredSchemes) as List<#T>",
                RuntimeTypes.Auth.Identity.AuthSchemeId,
                RuntimeTypes.Auth.Identity.AuthSchemeId,
            )

            write(
                "builder.config.bearerTokenProvider = " +
                    "builder.config.bearerTokenProvider ?: configureEnvironmentTokenProvider(provider)",
            )

            closeBlock("}")
            closeBlock("}")

            write("")

            // Configure a token provider that extracts the token from the target environment variable
            openBlock(
                "private fun configureEnvironmentTokenProvider(provider: #T): #T {",
                RuntimeTypes.Core.Utils.PlatformProvider,
                RuntimeTypes.Auth.HttpAuth.BearerTokenProvider,
            )

            openBlock(
                "return object : #T {",
                RuntimeTypes.Auth.HttpAuth.BearerTokenProvider,
            )

            openBlock(
                "override suspend fun resolve(attributes: #T): #T {",
                RuntimeTypes.Core.Collections.Attributes,
                RuntimeTypes.Auth.HttpAuth.BearerToken,
            )

            // Check environment variable on each resolve call
            write(
                "val bearerToken = provider.getenv(#S) ?: error(#S)",
                envVarName,
                "$envVarName environment variable is not set",
            )

            openBlock("return object : BearerToken {")

            write("override val token: String = bearerToken")
            write(
                "override val attributes: Attributes = #T()",
                RuntimeTypes.Core.Collections.emptyAttributes,
            )
            write(
                "override val expiration: #T? = null",
                RuntimeTypes.Core.Instant,
            )

            closeBlock("}")
            closeBlock("}")
            closeBlock("}")
            closeBlock("}")
        }
    }

    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(
                ServiceClientCompanionObjectWriter.FinalizeEnvironmentalConfig,
                finalizeEnvironmentTokenConfigConfigWriter,
            ),
        )

    private val finalizeEnvironmentTokenConfigConfigWriter = AppendingSectionWriter { writer ->
        val serviceName = writer.getContextValue(ServiceClientGenerator.Sections.CompanionObject.ServiceSymbol)
            .name
            .removeSuffix("Client")

        val authSchemePreference = buildSymbol {
            name = "finalize${serviceName}EnvironmentTokenConfig"
            namespace = "aws.sdk.kotlin.services.${serviceName.lowercase()}.auth"
        }
        writer.write("#T(builder)", authSchemePreference)
    }
}

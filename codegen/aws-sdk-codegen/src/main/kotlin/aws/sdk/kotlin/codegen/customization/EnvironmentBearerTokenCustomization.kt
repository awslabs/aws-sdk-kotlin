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
        val serviceName = clientName(ctx.settings.sdkId)
        val packageName = ctx.settings.pkg.name

        delegator.useFileWriter(
            "Finalize${serviceName}EnvironmentBearerTokenConfig.kt",
            "$packageName.auth",
        ) { writer ->
            renderEnvironmentBearerTokenConfig(
                writer,
                ctx,
                serviceShape,
                serviceName,
            )
        }
    }

    private fun renderEnvironmentBearerTokenConfig(
        writer: KotlinWriter,
        ctx: CodegenContext,
        serviceShape: ServiceShape,
        serviceName: String,
    ) {
        val serviceSymbol = ctx.symbolProvider.toSymbol(serviceShape)
        val signingServiceName = AwsSignatureVersion4.signingServiceName(serviceShape)
        // Transform signing name to environment variable name
        val envVarName = "AWS_BEARER_TOKEN_" + signingServiceName.replace("""[-\s]""".toRegex(), "_").uppercase()

        writer.apply {
            withBlock(
                "internal fun finalize#LEnvironmentBearerTokenConfig(",
                ")",
                serviceName,
            ) {
                write(
                    "builder: #T.Builder,",
                    serviceSymbol,
                )
                write(
                    "provider: #1T = #1T.System",
                    RuntimeTypes.Core.Utils.PlatformProvider,
                )
            }
            withBlock("{", "}") {
                // The customization do nothing if environment variable is not set
                withBlock(
                    "if (provider.getenv(#S) != null) {",
                    "}",
                    envVarName,
                ) {
                    // Configure auth scheme preference if customer hasn't specify one
                    write(
                        "builder.config.authSchemePreference = builder.config.authSchemePreference ?: listOf(#T.HttpBearer)",
                        RuntimeTypes.Auth.Identity.AuthSchemeId,
                    )

                    // Promote HttpBearer to first position in auth scheme preference list
                    withBlock(
                        "val filteredSchemes = builder.config.authSchemePreference?.filterNot {",
                        " }?: emptyList()",
                    ) {
                        write(
                            "it == #T.HttpBearer",
                            RuntimeTypes.Auth.Identity.AuthSchemeId,
                        )
                    }

                    write(
                        "builder.config.authSchemePreference = listOf(#1T.HttpBearer) + filteredSchemes",
                        RuntimeTypes.Auth.Identity.AuthSchemeId,
                    )

                    write(
                        "builder.config.bearerTokenProvider = " +
                            "builder.config.bearerTokenProvider ?: #T(#S, provider)",
                        RuntimeTypes.Auth.HttpAuth.EnvironmentBearerTokenProvider,
                        envVarName,
                    )
                }
            }
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
            name = "finalize${serviceName}EnvironmentBearerTokenConfig"
            namespace = "aws.sdk.kotlin.services.${serviceName.lowercase()}.auth"
        }

        writer.write("#T(builder)", environmentBearerTokenConfig)
    }
}

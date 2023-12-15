/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.kotlin.codegen.aws.customization

import software.amazon.smithy.aws.traits.ServiceTrait
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.getContextValue
import software.amazon.smithy.kotlin.codegen.integration.AppendingSectionWriter
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.integration.SectionWriterBinding
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.*
import software.amazon.smithy.kotlin.codegen.model.knowledge.AwsSignatureVersion4
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointCustomization
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolClientGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpProtocolUnitTestRequestGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.putIfAbsent
import software.amazon.smithy.kotlin.codegen.rendering.util.ConfigProperty
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.rulesengine.language.EndpointRuleSet
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter

/**
 * Registers support for the concept of region on the service client config, endpoint builtins, etc.
 *
 * Region is enabled IFF sigv4(a) is enabled or an AWS SDK service is targeted
 */
class RegionSupport : KotlinIntegration {
    companion object {
        const val BUILTIN_NAME = "AWS::Region"

        val RegionProp: ConfigProperty = ConfigProperty {
            name = "region"
            symbol = KotlinTypes.String.toBuilder().nullable().build()
            documentation = """
                The region to sign with and make requests to.
            """.trimIndent()
        }
    }

    // Allow other integrations to customize the service config props, later integrations take precedence.
    // This is used by AWS SDK codegen to customize the base class and documentation for this property
    override val order: Byte = -50

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val service = model.expectShape<ServiceShape>(settings.service)
        val supportsSigv4 = AwsSignatureVersion4.isSupportedAuthentication(model, service)
        val hasRegionBuiltin = service.getEndpointRules()?.parameters?.find { it.isBuiltIn && it.builtIn.get() == BUILTIN_NAME } != null
        val isAwsSdk = service.hasTrait<ServiceTrait>()
        return supportsSigv4 || hasRegionBuiltin || isAwsSdk
    }

    override fun additionalServiceConfigProps(ctx: CodegenContext): List<ConfigProperty> = listOf(RegionProp)

    override fun customizeEndpointResolution(ctx: ProtocolGenerator.GenerationContext): EndpointCustomization =
        object : EndpointCustomization {
            override fun renderBindEndpointBuiltins(
                ctx: ProtocolGenerator.GenerationContext,
                rules: EndpointRuleSet,
                writer: KotlinWriter,
            ) {
                val builtins = rules.parameters?.toList()?.filter(Parameter::isBuiltIn) ?: return
                builtins.forEach {
                    when (it.builtIn.get()) {
                        BUILTIN_NAME -> writer.write("#L = config.#L", it.defaultName(), RegionProp.propertyName)
                    }
                }
            }
        }
    override val sectionWriters: List<SectionWriterBinding>
        get() = listOf(
            SectionWriterBinding(HttpProtocolUnitTestRequestGenerator.ConfigureServiceClient, renderHttpProtocolRequestTestConfigureServiceClient),
            SectionWriterBinding(HttpProtocolClientGenerator.MergeServiceDefaults, renderRegionOperationContextDefault),
        )

    // sets a default region for protocol tests
    private val renderHttpProtocolRequestTestConfigureServiceClient = AppendingSectionWriter { writer ->
        // specify a default region
        writer.write("region = #S", "us-east-1")
    }

    // sets (initial) region/signing region in the execution context
    private val renderRegionOperationContextDefault = AppendingSectionWriter { writer ->
        val ctx = writer.getContextValue(HttpProtocolClientGenerator.MergeServiceDefaults.GenerationContext)
        val isAwsSdk = ctx.service.hasTrait<ServiceTrait>()

        if (isAwsSdk) {
            val awsClientOption = buildSymbol {
                name = "AwsClientOption"
                namespace = "aws.sdk.kotlin.runtime.client"
            }
            writer.putIfAbsent(awsClientOption, "Region", nullable = true)
        }

        writer.putIfAbsent(
            RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningAttributes,
            "SigningRegion",
            "config.region",
            nullable = true,
        )
    }
}

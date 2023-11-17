/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.protocols.endpoints

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.AwsServiceConfigIntegration
import aws.sdk.kotlin.codegen.customization.AccountIdEndpointBuiltinCustomization
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.defaultName
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointParametersGenerator
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointResolverAdapterGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.rulesengine.language.EndpointRuleSet
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter
import aws.sdk.kotlin.codegen.customization.s3.ClientConfigIntegration as S3ClientConfigIntegration
import aws.sdk.kotlin.codegen.customization.s3control.ClientConfigIntegration as S3ControlClientConfigIntegration

/**
 * Integration that renders binding AWS builtin endpoint parameters
 */
class BindAwsEndpointBuiltins : KotlinIntegration {
    override fun renderBindEndpointBuiltins(
        ctx: ProtocolGenerator.GenerationContext,
        rules: EndpointRuleSet,
        writer: KotlinWriter,
    ) {
        val builtins = rules.parameters?.toList()?.filter(Parameter::isBuiltIn) ?: return
        writer.write("#T(config, request)", bindAwsBuiltinsSymbol(ctx, builtins))
    }
}

/**
 * Render binding of AWS SDK endpoint parameter builtins. In practice, all of these values are sourced from the client
 * config.
 */
fun renderBindAwsBuiltins(ctx: ProtocolGenerator.GenerationContext, writer: KotlinWriter, builtinParams: List<Parameter>) {
    writer.withBlock(
        "private fun #T.Builder.bindAwsBuiltins(config: #T.Config, request: #T) {",
        "}",
        EndpointParametersGenerator.getSymbol(ctx.settings),
        ctx.symbolProvider.toSymbol(ctx.service),
        RuntimeTypes.HttpClient.Operation.ResolveEndpointRequest,
    ) {
        builtinParams.forEach {
            when (it.builtIn.get()) {
                AwsBuiltins.REGION -> renderBasicConfigBinding(writer, it, AwsServiceConfigIntegration.RegionProp.propertyName)
                AwsBuiltins.USE_FIPS -> renderBasicConfigBinding(writer, it, AwsServiceConfigIntegration.UseFipsProp.propertyName)
                AwsBuiltins.USE_DUAL_STACK -> renderBasicConfigBinding(writer, it, AwsServiceConfigIntegration.UseDualStackProp.propertyName)

                AwsBuiltins.S3_ACCELERATE -> renderBasicConfigBinding(writer, it, S3ClientConfigIntegration.EnableAccelerateProp.propertyName)
                AwsBuiltins.S3_FORCE_PATH_STYLE -> renderBasicConfigBinding(writer, it, S3ClientConfigIntegration.ForcePathStyleProp.propertyName)
                AwsBuiltins.S3_DISABLE_MRAP -> renderBasicConfigBinding(writer, it, S3ClientConfigIntegration.DisableMrapProp.propertyName)
                AwsBuiltins.S3_USE_ARN_REGION -> renderBasicConfigBinding(writer, it, S3ClientConfigIntegration.UseArnRegionProp.propertyName)
                AwsBuiltins.S3_CONTROL_USE_ARN_REGION -> renderBasicConfigBinding(writer, it, S3ControlClientConfigIntegration.UseArnRegionProp.propertyName)

                AwsBuiltins.SDK_ENDPOINT ->
                    writer.write("#L = config.#L?.toString()", it.defaultName(), AwsServiceConfigIntegration.EndpointUrlProp.propertyName)

                // as a newer SDK we do NOT support these values, they are always false
                AwsBuiltins.S3_USE_GLOBAL_ENDPOINT, AwsBuiltins.STS_USE_GLOBAL_ENDPOINT ->
                    writer.write("#L = false", it.defaultName())

                AwsBuiltins.ACCOUNT_ID ->
                    writer.write(
                        "#L = #T(config.#L, request.identity.attributes)",
                        it.defaultName(),
                        AwsRuntimeTypes.Config.Endpoints.resolveAccountId,
                        AccountIdEndpointBuiltinCustomization.AccountIdEndpointModeProp.propertyName,
                    )
            }
        }
    }
}

fun bindAwsBuiltinsSymbol(ctx: ProtocolGenerator.GenerationContext, builtinParams: List<Parameter>): Symbol =
    buildSymbol {
        name = "bindAwsBuiltins"
        namespace = "${ctx.settings.pkg.name}.endpoints.internal"
        definitionFile = EndpointResolverAdapterGenerator.getSymbol(ctx.settings).definitionFile
        renderBy = { writer ->
            renderBindAwsBuiltins(ctx, writer, builtinParams)
        }
    }

private fun renderBasicConfigBinding(writer: KotlinWriter, param: Parameter, configMember: String) {
    writer.write("#L = config.#L", param.defaultName(), configMember)
}

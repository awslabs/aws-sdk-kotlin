/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.protocols.endpoints

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointParametersGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter

/**
 * Render binding of AWS SDK endpoint parameter builtins. In practice, all of these values are sourced from the client
 * config.
 */
fun renderBindAwsBuiltins(ctx: ProtocolGenerator.GenerationContext, writer: KotlinWriter, builtinParams: List<Parameter>) {
    writer.withBlock(
        "public fun #T.Builder.bindAwsBuiltins(config: #T.Config) {",
        "}",
        EndpointParametersGenerator.getSymbol(ctx.settings),
        ctx.symbolProvider.toSymbol(ctx.service),
    ) {
        builtinParams.forEach {
            when (it.builtIn.get()) {
                "AWS::Region" -> renderBasicConfigBinding(writer, it, "region")
                "AWS::UseFIPS" -> renderBasicConfigBinding(writer, it, "useFips")
                "AWS::UseDualStack" -> renderBasicConfigBinding(writer, it, "useDualStack")

                "AWS::S3::Accelerate" -> renderBasicConfigBinding(writer, it, "enableAccelerate")
                "AWS::S3::ForcePathStyle" -> renderBasicConfigBinding(writer, it, "forcePathStyle")
                "AWS::S3::DisableMultiRegionAccessPoints" -> renderBasicConfigBinding(writer, it, "disableMrap")
                "AWS::S3::UseArnRegion", "AWS::S3Control::UseArnRegion" -> renderBasicConfigBinding(writer, it, "useArnRegion")

                "SDK::Endpoint" ->
                    writer.write("#L = config.endpointUrl?.toString()", EndpointParametersGenerator.getParamKotlinName(it))

                // as a newer SDK we do NOT support these values, they are always false
                "AWS::S3::UseGlobalEndpoint", "AWS::STS::UseGlobalEndpoint" ->
                    writer.write("#L = false", EndpointParametersGenerator.getParamKotlinName(it))
            }
        }
    }
}

fun bindAwsBuiltinsSymbol(settings: KotlinSettings): Symbol =
    buildSymbol {
        name = "bindAwsBuiltins"
        namespace = "${settings.pkg.name}.endpoints.internal"
    }

private fun renderBasicConfigBinding(writer: KotlinWriter, param: Parameter, configMember: String) {
    writer.write("#L = config.#L", EndpointParametersGenerator.getParamKotlinName(param), configMember)
}

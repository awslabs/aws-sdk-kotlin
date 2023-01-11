/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.protocols.endpoints

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointParametersGenerator
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointProviderGenerator

fun EndpointProviderGenerator.Companion.renderAsSigningProviderExt(settings: KotlinSettings, writer: KotlinWriter) {
    writer.withBlock(
        "internal fun #1T<#2T>.asSigningProvider(params: #2T): #3T = {",
        "}",
        RuntimeTypes.Http.Endpoints.EndpointProvider,
        EndpointParametersGenerator.getSymbol(settings),
        RuntimeTypes.Auth.Signing.AwsSigningCommon.SigningEndpointProvider,
    ) {
        write("val endpoint = resolveEndpoint(params)")
        write(
            "#T(endpoint, endpoint.#T?.#T())",
            RuntimeTypes.Auth.Signing.AwsSigningCommon.SigningContextualizedEndpoint,
            AwsRuntimeTypes.Endpoint.authSchemeEndpointExt,
            AwsRuntimeTypes.Endpoint.asSigningContextAuthSchemeExt,
        )
    }
}

fun EndpointProviderGenerator.Companion.getAsSigningProviderExtSymbol(settings: KotlinSettings) =
    buildSymbol {
        name = "asSigningProvider"
        namespace = getSymbol(settings).namespace
    }

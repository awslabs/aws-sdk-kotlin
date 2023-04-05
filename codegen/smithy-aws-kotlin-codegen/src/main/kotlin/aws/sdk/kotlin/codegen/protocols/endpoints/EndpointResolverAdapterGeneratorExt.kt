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
import software.amazon.smithy.kotlin.codegen.rendering.endpoints.EndpointResolverAdapterGenerator

fun EndpointResolverAdapterGenerator.Companion.renderAsSigningProviderExt(settings: KotlinSettings, writer: KotlinWriter) {
    writer.withBlock(
        "internal fun #T.asSigningProvider(input: Any, operationName: String): #T = {",
        "}",
        getSymbol(settings),
        RuntimeTypes.Auth.Signing.AwsSigningCommon.SigningEndpointProvider,
    ) {
        // re-use the endpoint provider adapter to get an endpoint, we need to feed it a representative request
        // including mandatory execution context
        withBlock("val execContext = #T().apply {", "}", RuntimeTypes.Core.ExecutionContext) {
            write("set(#T.OperationName, operationName)", RuntimeTypes.SmithyClient.SdkClientOption)
            write("set(#T.OperationInput, input)", RuntimeTypes.HttpClient.Operation.HttpOperationContext)
        }
        write("val httpReq = #T().build()", RuntimeTypes.Http.Request.HttpRequestBuilder)
        write("val request = #T(execContext, httpReq, #T)", RuntimeTypes.HttpClient.Operation.ResolveEndpointRequest, RuntimeTypes.Auth.HttpAuth.AnonymousIdentity)
        write("val endpoint = resolve(request)")
        write(
            "#T(endpoint, endpoint.#T?.#T())",
            RuntimeTypes.Auth.Signing.AwsSigningCommon.SigningContextualizedEndpoint,
            AwsRuntimeTypes.Endpoint.authSchemeEndpointExt,
            AwsRuntimeTypes.Endpoint.asSigningContextAuthSchemeExt,
        )
    }
}

fun EndpointResolverAdapterGenerator.Companion.getAsSigningProviderExtSymbol(settings: KotlinSettings) =
    buildSymbol {
        name = "asSigningProvider"
        namespace = "${settings.pkg.name}.presigners"
    }

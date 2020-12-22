/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinDependency
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.addImport
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingProtocolGenerator
import software.amazon.smithy.kotlin.codegen.integration.HttpFeature
import software.amazon.smithy.kotlin.codegen.integration.HttpProtocolClientGenerator
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator

/**
 * Base class for all AWS HTTP protocol generators
 */
abstract class AwsHttpBindingProtocolGenerator : HttpBindingProtocolGenerator() {

    override val exceptionBaseClassSymbol: Symbol = Symbol.builder()
        .name("AwsServiceException")
        .namespace(AwsKotlinDependency.AWS_CLIENT_RT_CORE.namespace, ".")
        .addDependency(AwsKotlinDependency.AWS_CLIENT_RT_CORE)
        .build()

    override fun getHttpProtocolClientGenerator(ctx: ProtocolGenerator.GenerationContext): HttpProtocolClientGenerator {
        val rootNamespace = ctx.settings.moduleName
        val features = getHttpFeatures(ctx)
        return AwsHttpProtocolClientGenerator(ctx, rootNamespace, features)
    }

    override fun getHttpFeatures(ctx: ProtocolGenerator.GenerationContext): List<HttpFeature> {
        val features = super.getHttpFeatures(ctx).toMutableList()
        features.add(DummyEndpointResolver())
        if (AwsSignatureVersion4.isSupportedAuthentication(ctx.model, ctx.service)) {
            val signingName = AwsSignatureVersion4.signingServiceName(ctx.model, ctx.service)
            features.add(AwsSignatureVersion4(signingName))
        }
        return features
    }
}

// FIXME - this is temporary hack to generate a working service. This sets the host to make a service request against.
// This needs designed as a more generic middleware that deals with endpoint resolution:
// ticket: https://www.pivotaltracker.com/story/show/174869500
class DummyEndpointResolver : HttpFeature {
    override val name: String = "DefaultRequest"

    override fun addImportsAndDependencies(writer: KotlinWriter) {
        writer.addImport("DefaultRequest", KotlinDependency.CLIENT_RT_HTTP, "${KotlinDependency.CLIENT_RT_HTTP.namespace}.feature")
    }

    override fun renderConfigure(writer: KotlinWriter) {
        writer.writeWithNoFormatting("url.host = \"\${serviceName.toLowerCase()}.\${config.region}.amazonaws.com\"")
        writer.write("url.scheme = Protocol.HTTPS")
        writer.write("headers.append(\"Host\", url.host)")
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.core.KotlinDependency
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace

/**
 * Commonly used AWS runtime types. Provides a single definition of a runtime symbol such that codegen isn't littered
 * with inline symbol creation which makes refactoring of the runtime more difficult and error prone.
 *
 * NOTE: Not all symbols need be added here but it doesn't hurt to define runtime symbols once.
 */
object AwsRuntimeTypes {
    object Core {
        val AwsClientOption = runtimeSymbol("AwsClientOption", AwsKotlinDependency.AWS_CORE, "client")
        val AuthAttributes = runtimeSymbol("AuthAttributes", AwsKotlinDependency.AWS_CORE, "execution")
        val AwsErrorMetadata = runtimeSymbol("AwsErrorMetadata", AwsKotlinDependency.AWS_CORE)
        val UnknownServiceErrorException = runtimeSymbol("UnknownServiceErrorException", AwsKotlinDependency.AWS_CORE)
        val ClientException = runtimeSymbol("ClientException", AwsKotlinDependency.AWS_CORE)

        object Endpoint {
            val EndpointResolver = runtimeSymbol("EndpointResolver", AwsKotlinDependency.AWS_CORE, "endpoint")
            val Endpoint = runtimeSymbol("Endpoint", AwsKotlinDependency.AWS_CORE, "endpoint")

            object Internal {
                val CredentialScope = runtimeSymbol("CredentialScope", AwsKotlinDependency.AWS_CORE, "endpoint.internal")
                val EndpointDefinition = runtimeSymbol("EndpointDefinition", AwsKotlinDependency.AWS_CORE, "endpoint.internal")
                val Partition = runtimeSymbol("Partition", AwsKotlinDependency.AWS_CORE, "endpoint.internal")
                val resolveEndpoint = runtimeSymbol("resolveEndpoint", AwsKotlinDependency.AWS_CORE, "endpoint.internal")
            }
        }
    }

    object Auth {
        val AwsSigV4SigningMiddleware = runtimeSymbol("AwsSigV4SigningMiddleware", AwsKotlinDependency.AWS_AUTH)
        val AwsSignedBodyHeaderType = runtimeSymbol("AwsSignedBodyHeaderType", AwsKotlinDependency.AWS_AUTH)
        val CredentialsProvider = runtimeSymbol("CredentialsProvider", AwsKotlinDependency.AWS_AUTH)
        val createPresignedRequest = runtimeSymbol("createPresignedRequest", AwsKotlinDependency.AWS_AUTH)
        val DefaultChainCredentialsProvider = runtimeSymbol("DefaultChainCredentialsProvider", AwsKotlinDependency.AWS_AUTH)
        val PresignedRequest = runtimeSymbol("PresignedRequest", AwsKotlinDependency.AWS_AUTH)
        val PresignedRequestConfig = runtimeSymbol("PresignedRequestConfig", AwsKotlinDependency.AWS_AUTH)
        val ServicePresignConfig = runtimeSymbol("ServicePresignConfig", AwsKotlinDependency.AWS_AUTH)
        val SigningLocation = runtimeSymbol("SigningLocation", AwsKotlinDependency.AWS_AUTH)
    }

    object Regions {
        val DefaultAwsRegionProviderChain = runtimeSymbol("DefaultAwsRegionProviderChain", AwsKotlinDependency.AWS_REGIONS, "providers")
    }

    object Http {
        val withPayload = runtimeSymbol("withPayload", AwsKotlinDependency.AWS_HTTP)
        val setAseErrorMetadata = runtimeSymbol("setAseErrorMetadata", AwsKotlinDependency.AWS_HTTP)
    }

    object JsonProtocols {
        val RestJsonErrorDeserializer = runtimeSymbol("RestJsonErrorDeserializer", AwsKotlinDependency.AWS_JSON_PROTOCOLS)
    }

    object XmlProtocols {
        val parseRestXmlErrorResponse = runtimeSymbol("parseRestXmlErrorResponse", AwsKotlinDependency.AWS_XML_PROTOCOLS)
        val parseEc2QueryErrorResponse = runtimeSymbol("parseEc2QueryErrorResponse", AwsKotlinDependency.AWS_XML_PROTOCOLS)
    }
}

private fun runtimeSymbol(name: String, dependency: KotlinDependency, subpackage: String = ""): Symbol = buildSymbol {
    this.name = name
    namespace(dependency, subpackage)
}

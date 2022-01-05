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
        val ClientException = runtimeSymbol("ClientException", AwsKotlinDependency.AWS_CORE)
    }

    object Endpoint {
        val AwsEndpointResolver = runtimeSymbol("AwsEndpointResolver", AwsKotlinDependency.AWS_ENDPOINT)
        val AwsEndpoint = runtimeSymbol("AwsEndpoint", AwsKotlinDependency.AWS_ENDPOINT)
        val CredentialScope = runtimeSymbol("CredentialScope", AwsKotlinDependency.AWS_ENDPOINT)

        object Internal {
            val EndpointDefinition = runtimeSymbol("EndpointDefinition", AwsKotlinDependency.AWS_ENDPOINT, "internal")
            val Partition = runtimeSymbol("Partition", AwsKotlinDependency.AWS_ENDPOINT, "internal")
            val resolveEndpoint = runtimeSymbol("resolveEndpoint", AwsKotlinDependency.AWS_ENDPOINT, "internal")
        }
    }

    object Types {
        val CredentialsProvider = runtimeSymbol("CredentialsProvider", AwsKotlinDependency.AWS_TYPES, "auth.credentials")
        val Credentials = runtimeSymbol("Credentials", AwsKotlinDependency.AWS_TYPES, "auth.credentials")
        val AwsClientConfig = runtimeSymbol("AwsClientConfig", AwsKotlinDependency.AWS_TYPES, "client")
    }

    object Config {
        object Credentials {
            val DefaultChainCredentialsProvider = runtimeSymbol("DefaultChainCredentialsProvider", AwsKotlinDependency.AWS_CONFIG, "auth.credentials")
            val StaticCredentialsProvider = runtimeSymbol("StaticCredentialsProvider", AwsKotlinDependency.AWS_CONFIG, "auth.credentials")
        }

        val AwsClientConfigLoadOptions = runtimeSymbol("AwsClientConfigLoadOptions", AwsKotlinDependency.AWS_CONFIG, "config")
        val fromEnvironment = runtimeSymbol("fromEnvironment", AwsKotlinDependency.AWS_CONFIG, "config")
    }

    object Signing {
        val AwsSigV4SigningMiddleware = runtimeSymbol("AwsSigV4SigningMiddleware", AwsKotlinDependency.AWS_SIGNING)
        val AwsSignedBodyHeaderType = runtimeSymbol("AwsSignedBodyHeaderType", AwsKotlinDependency.AWS_SIGNING)
        val createPresignedRequest = runtimeSymbol("createPresignedRequest", AwsKotlinDependency.AWS_SIGNING)
        val PresignedRequestConfig = runtimeSymbol("PresignedRequestConfig", AwsKotlinDependency.AWS_SIGNING)
        val ServicePresignConfig = runtimeSymbol("ServicePresignConfig", AwsKotlinDependency.AWS_SIGNING)
        val SigningLocation = runtimeSymbol("SigningLocation", AwsKotlinDependency.AWS_SIGNING)
    }

    object Http {
        val withPayload = runtimeSymbol("withPayload", AwsKotlinDependency.AWS_HTTP)
        val setAseErrorMetadata = runtimeSymbol("setAseErrorMetadata", AwsKotlinDependency.AWS_HTTP)

        object Retries {
            val AwsDefaultRetryPolicy = runtimeSymbol("AwsDefaultRetryPolicy", AwsKotlinDependency.AWS_HTTP, "retries")
        }
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

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
        val AwsErrorMetadata = runtimeSymbol("AwsErrorMetadata", AwsKotlinDependency.AWS_CORE)
        val ClientException = runtimeSymbol("ClientException", AwsKotlinDependency.AWS_CORE)
    }

    object Endpoint {
        val asSigningEndpointProvider = runtimeSymbol("asSigningEndpointProvider", AwsKotlinDependency.AWS_ENDPOINT)
        val AwsEndpointResolver = runtimeSymbol("AwsEndpointResolver", AwsKotlinDependency.AWS_ENDPOINT)
        val AwsEndpoint = runtimeSymbol("AwsEndpoint", AwsKotlinDependency.AWS_ENDPOINT)
        val CredentialScope = runtimeSymbol("CredentialScope", AwsKotlinDependency.AWS_ENDPOINT)

        object Internal {
            val EndpointDefinition = runtimeSymbol("EndpointDefinition", AwsKotlinDependency.AWS_ENDPOINT, "internal")
            val Partition = runtimeSymbol("Partition", AwsKotlinDependency.AWS_ENDPOINT, "internal")
            val resolveEndpoint = runtimeSymbol("resolveEndpoint", AwsKotlinDependency.AWS_ENDPOINT, "internal")
        }
    }

    object Config {
        object Credentials {
            val DefaultChainCredentialsProvider = runtimeSymbol("DefaultChainCredentialsProvider", AwsKotlinDependency.AWS_CONFIG, "auth.credentials")
            val StaticCredentialsProvider = runtimeSymbol("StaticCredentialsProvider", AwsKotlinDependency.AWS_CONFIG, "auth.credentials")
            val borrow = runtimeSymbol("borrow", AwsKotlinDependency.AWS_CONFIG, "auth.credentials.internal")
        }

        object Region {
            val resolveRegion = runtimeSymbol("resolveRegion", AwsKotlinDependency.AWS_CONFIG, "region")
        }
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

    object AwsEventStream {
        val HeaderValue = runtimeSymbol("HeaderValue", AwsKotlinDependency.AWS_EVENT_STREAM)
        val Message = runtimeSymbol("Message", AwsKotlinDependency.AWS_EVENT_STREAM)
        val MessageType = runtimeSymbol("MessageType", AwsKotlinDependency.AWS_EVENT_STREAM)
        val MessageTypeExt = runtimeSymbol("type", AwsKotlinDependency.AWS_EVENT_STREAM)

        val asEventStreamHttpBody = runtimeSymbol("asEventStreamHttpBody", AwsKotlinDependency.AWS_EVENT_STREAM)
        val buildMessage = runtimeSymbol("buildMessage", AwsKotlinDependency.AWS_EVENT_STREAM)
        val decodeFrames = runtimeSymbol("decodeFrames", AwsKotlinDependency.AWS_EVENT_STREAM)
        val encode = runtimeSymbol("encode", AwsKotlinDependency.AWS_EVENT_STREAM)

        val expectBool = runtimeSymbol("expectBool", AwsKotlinDependency.AWS_EVENT_STREAM)
        val expectByte = runtimeSymbol("expectByte", AwsKotlinDependency.AWS_EVENT_STREAM)
        val expectByteArray = runtimeSymbol("expectByteArray", AwsKotlinDependency.AWS_EVENT_STREAM)
        val expectInt16 = runtimeSymbol("expectInt16", AwsKotlinDependency.AWS_EVENT_STREAM)
        val expectInt32 = runtimeSymbol("expectInt32", AwsKotlinDependency.AWS_EVENT_STREAM)
        val expectInt64 = runtimeSymbol("expectInt64", AwsKotlinDependency.AWS_EVENT_STREAM)
        val expectTimestamp = runtimeSymbol("expectTimestamp", AwsKotlinDependency.AWS_EVENT_STREAM)
        val expectString = runtimeSymbol("expectString", AwsKotlinDependency.AWS_EVENT_STREAM)

        val sign = runtimeSymbol("sign", AwsKotlinDependency.AWS_EVENT_STREAM)
        val newEventStreamSigningConfig = runtimeSymbol("newEventStreamSigningConfig", AwsKotlinDependency.AWS_EVENT_STREAM)
    }
}

private fun runtimeSymbol(name: String, dependency: KotlinDependency, subpackage: String = ""): Symbol = buildSymbol {
    this.name = name
    namespace(dependency, subpackage)
}

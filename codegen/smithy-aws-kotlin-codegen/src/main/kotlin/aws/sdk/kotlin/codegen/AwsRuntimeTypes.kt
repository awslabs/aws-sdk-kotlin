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
    }
}

private fun runtimeSymbol(name: String, dependency: KotlinDependency, subpackage: String = ""): Symbol = buildSymbol {
    this.name = name
    namespace(dependency, subpackage)
}

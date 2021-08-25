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
object SdkRuntimeTypes {
    object Core {
        val UnknownServiceErrorException = runtimeSymbol("UnknownServiceErrorException", AwsKotlinDependency.AWS_CORE)
        val ClientException = runtimeSymbol("ClientException", AwsKotlinDependency.AWS_CORE)
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

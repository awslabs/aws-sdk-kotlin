/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.KotlinDependency
import software.amazon.smithy.kotlin.codegen.buildSymbol
import software.amazon.smithy.kotlin.codegen.namespace

/**
 * Commonly used AWS runtime types. Provides a single definition of a runtime symbol such that codegen isn't littered
 * with inline symbol creation which makes refactoring of the runtime more difficult and error prone.
 *
 * NOTE: Not all symbols need be added here but it doesn't hurt to define runtime symbols once.
 */
object AwsRuntimeTypes {
    object Core {
        val AwsClientOption = buildSymbol {
            name = "AwsClientOption"
            namespace(AwsKotlinDependency.AWS_CLIENT_RT_CORE, subpackage = "client")
        }

        val AuthAttributes = buildSymbol {
            name = "AuthAttributes"
            namespace(AwsKotlinDependency.AWS_CLIENT_RT_CORE, subpackage = "execution")
        }
    }

    object SerdeXml {
        val XmlSerialName = buildSymbol {
            name = "XmlSerialName"
            namespace(KotlinDependency.CLIENT_RT_SERDE_XML)
        }

        val XmlNamespace = buildSymbol {
            name = "XmlNamespace"
            namespace(KotlinDependency.CLIENT_RT_SERDE_XML)
        }

        val Flattened = buildSymbol {
            name = "Flattened"
            namespace(KotlinDependency.CLIENT_RT_SERDE_XML)
        }

        val XmlAttribute = buildSymbol {
            name = "XmlAttribute"
            namespace(KotlinDependency.CLIENT_RT_SERDE_XML)
        }
    }
}

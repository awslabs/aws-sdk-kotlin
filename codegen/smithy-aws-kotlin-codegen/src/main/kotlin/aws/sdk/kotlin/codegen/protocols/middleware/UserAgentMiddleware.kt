/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.middleware

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.protocol.HttpFeatureMiddleware

/**
 * Middleware that sets the User-Agent and x-amz-user-agent headers
 */
class UserAgentMiddleware : HttpFeatureMiddleware() {
    override val name: String = "UserAgent"
    override val order: Byte = 20

    private val uaSymbol = buildSymbol {
        name = "AwsUserAgentMetadata"
        namespace(AwsKotlinDependency.AWS_HTTP)
    }

    private val apiMetaSymbol = buildSymbol {
        name = "ApiMetadata"
        namespace(AwsKotlinDependency.AWS_HTTP)
    }
    private val featSymbol = buildSymbol {
        name = "UserAgent"
        namespace(AwsKotlinDependency.AWS_HTTP, subpackage = "middleware")
    }

    override fun renderProperties(writer: KotlinWriter) {
        // static metadata that doesn't change per/request
        writer.addImport(uaSymbol)
        writer.addImport(apiMetaSymbol)
        writer.addImport(featSymbol)
        writer.write("private val awsUserAgentMetadata = #T.fromEnvironment(#T(ServiceId, SdkVersion))", uaSymbol, apiMetaSymbol)
    }

    override fun renderConfigure(writer: KotlinWriter) {
        writer.write("staticMetadata = awsUserAgentMetadata")
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.middleware

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware

/**
 * Middleware that sets the User-Agent and x-amz-user-agent headers
 */
class UserAgentMiddleware : ProtocolMiddleware {
    override val name: String = "UserAgent"

    private val uaSymbol = buildSymbol {
        name = "AwsUserAgentMetadata"
        namespace(AwsKotlinDependency.AWS_CLIENT_RT_HTTP)
    }

    private val apiMetaSymbol = buildSymbol {
        name = "ApiMetadata"
        namespace(AwsKotlinDependency.AWS_CLIENT_RT_HTTP)
    }
    private val featSymbol = buildSymbol {
        name = "UserAgent"
        namespace(AwsKotlinDependency.AWS_CLIENT_RT_HTTP, subpackage = "middleware")
    }

    override fun renderProperties(writer: KotlinWriter) {
        // user agent metadata (in general) shouldn't be changing per/request
        writer.addImport(uaSymbol)
        writer.addImport(apiMetaSymbol)
        writer.addImport(featSymbol)
        writer.write("private val awsUserAgentMetadata = #T.fromEnvironment(#T(ServiceId, SdkVersion))", uaSymbol, apiMetaSymbol)
    }

    override fun renderConfigure(writer: KotlinWriter) {
        writer.write("metadata = awsUserAgentMetadata")
    }
}

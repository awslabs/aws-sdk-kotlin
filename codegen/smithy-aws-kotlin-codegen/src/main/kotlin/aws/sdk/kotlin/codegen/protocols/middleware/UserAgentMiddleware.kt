/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.codegen.protocols.middleware

import aws.sdk.kotlin.codegen.AwsKotlinDependency
import aws.sdk.kotlin.codegen.protocols.core.ExtraMetadataGenerator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.namespace
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.shapes.OperationShape

/**
 * Middleware that sets the User-Agent and x-amz-user-agent headers
 */
class UserAgentMiddleware(ctx: ProtocolGenerator.GenerationContext) : ProtocolMiddleware {
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
    private val middlewareSymbol = buildSymbol {
        name = "UserAgent"
        namespace(AwsKotlinDependency.AWS_HTTP, subpackage = "middleware")
    }

    private val extraMetadataSymbol = ExtraMetadataGenerator.getSymbol(ctx.settings)

    override fun renderProperties(writer: KotlinWriter) {
        // static metadata that doesn't change per/request
        writer.withBlock("private val awsUserAgentMetadata =", "") {
            writer.write(
                "#T.fromEnvironment(#T(ServiceId, SdkVersion), #T)",
                uaSymbol,
                apiMetaSymbol,
                extraMetadataSymbol,
            )
        }
    }

    override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.write("op.install(#T(awsUserAgentMetadata))", middlewareSymbol)
    }
}

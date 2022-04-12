/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen.protocols.middleware

import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolGenerator
import software.amazon.smithy.kotlin.codegen.rendering.protocol.ProtocolMiddleware
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.ServiceIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.OptionalAuthTrait

/**
 * AWS Signature Version 4 Signing Middleware
 * @param service The credential scope service name to sign for
 * See the `name` property of: https://awslabs.github.io/smithy/1.0/spec/aws/aws-auth.html#aws-auth-sigv4-trait
 */
open class AwsSignatureVersion4(private val service: String) : ProtocolMiddleware {
    override val name: String = RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningMiddleware.name
    override val order: Byte = 126 // Must come before GlacierBodyChecksum

    init {
        require(service.isNotEmpty()) { "service name must be specified" }
    }

    override fun isEnabledFor(ctx: ProtocolGenerator.GenerationContext, op: OperationShape): Boolean {
        val service = ctx.model.expectShape<ServiceShape>(ctx.settings.service)
        return hasSigV4AuthScheme(ctx.model, service, op)
    }

    final override fun render(ctx: ProtocolGenerator.GenerationContext, op: OperationShape, writer: KotlinWriter) {
        writer.addImport(RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningMiddleware)

        writer.withBlock("op.install(", ")") {
            withBlock("#T {", "}", RuntimeTypes.Auth.Signing.AwsSigningCommon.AwsSigningMiddleware) {
                renderSigningConfig(op, writer)
            }
        }
    }

    protected open fun renderSigningConfig(op: OperationShape, writer: KotlinWriter) {
        writer.write("this.signer = config.signer")
        writer.write("this.credentialsProvider = config.credentialsProvider")
        writer.write("this.service = #S", service)
    }

    companion object {
        /**
         * Returns if the SigV4Trait is a auth scheme supported by the service.
         *
         * @param model        model definition
         * @param serviceShape service shape for the API
         * @return if the SigV4 trait is used by the service.
         */
        fun isSupportedAuthentication(model: Model, serviceShape: ServiceShape): Boolean =
            ServiceIndex
                .of(model)
                .getAuthSchemes(serviceShape)
                .values
                .any { it.javaClass == SigV4Trait::class.java }

        /**
         * Get the SigV4Trait auth name to sign request for
         *
         * @param serviceShape service shape for the API
         * @return the service name to use in the credential scope to sign for
         */
        fun signingServiceName(serviceShape: ServiceShape): String {
            val sigv4Trait = serviceShape.expectTrait<SigV4Trait>()
            return sigv4Trait.name
        }

        /**
         * Returns if the SigV4Trait is a auth scheme for the service and operation.
         *
         * @param model     model definition
         * @param service   service shape for the API
         * @param operation operation shape
         * @return if SigV4Trait is an auth scheme for the operation and service.
         */
        fun hasSigV4AuthScheme(model: Model, service: ServiceShape, operation: OperationShape): Boolean {
            val auth = ServiceIndex.of(model).getEffectiveAuthSchemes(service.id, operation.id)
            return auth.containsKey(SigV4Trait.ID) && !operation.hasTrait<OptionalAuthTrait>()
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.codegen

import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.kotlin.codegen.KotlinWriter
import software.amazon.smithy.kotlin.codegen.addImport
import software.amazon.smithy.kotlin.codegen.integration.HttpFeature
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.knowledge.ServiceIndex
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.traits.OptionalAuthTrait

/**
 * AWS Signature Version 4 Signing Middleware
 * @param signingServiceName The credential scope service name to sign for
 * See the `name` property of: https://awslabs.github.io/smithy/1.0/spec/aws/aws-auth.html#aws-auth-sigv4-trait
 */
class AwsSignatureVersion4(private val signingServiceName: String) : HttpFeature {
    override val name: String = "AwsSigv4Signer"

    init {
        require(signingServiceName.isNotEmpty()) { "signingServiceName must be specified" }
    }

    override fun addImportsAndDependencies(writer: KotlinWriter) {
        writer.addImport("AwsSigv4Signer", AwsKotlinDependency.AWS_CLIENT_RT_AUTH)
        writer.addImport("DefaultChainCredentialsProvider", AwsKotlinDependency.AWS_CLIENT_RT_AUTH)
    }

    override fun renderConfigure(writer: KotlinWriter) {
        writer.write("credentialsProvider = config.credentialsProvider ?: DefaultChainCredentialsProvider()")
        writer.write("signingService = #S", signingServiceName)
    }

    companion object {
        /**
         * Returns if the SigV4Trait is a auth scheme supported by the service.
         *
         * @param model        model definition
         * @param serviceShape service shape for the API
         * @return if the SigV4 trait is used by the service.
         */
        fun isSupportedAuthentication(model: Model, serviceShape: ServiceShape): Boolean {
            return ServiceIndex.of(model).getAuthSchemes(serviceShape).values
                .any { it.javaClass == SigV4Trait::class.java }
        }

        /**
         * Get the SigV4Trait auth name to sign request for
         *
         * @param model        model definition
         * @param serviceShape service shape for the API
         * @return the service name to use in the credential scope to sign for
         */
        fun signingServiceName(model: Model, serviceShape: ServiceShape): String {
            val sigv4Trait = serviceShape.expectTrait(SigV4Trait::class.java)
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
            return auth.containsKey(SigV4Trait.ID) && !operation.hasTrait(OptionalAuthTrait::class.java)
        }
    }
}

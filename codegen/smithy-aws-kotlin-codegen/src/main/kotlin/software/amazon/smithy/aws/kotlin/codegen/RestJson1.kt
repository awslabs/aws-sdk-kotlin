/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.amazon.smithy.aws.kotlin.codegen

import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.kotlin.codegen.integration.DefaultHttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.HttpBindingResolver
import software.amazon.smithy.kotlin.codegen.integration.ProtocolGenerator
import software.amazon.smithy.model.shapes.ShapeId

// The default Http Binding resolver is used for both white-label smithy-kotlin tests
// and as the restJson1 binding resolver.  If/when AWS-specific logic needs to
// be added to the resolver which is not "white label" in character, these types
// should be broken into two: one purely scoped for white-label SDK testing and one
// for restJson1 support.
typealias RestJsonHttpBindingResolver = DefaultHttpBindingResolver

/**
 * Handles generating the aws.protocols#restJson1 protocol for services.
 *
 * @inheritDoc
 * @see RestJsonProtocolGenerator
 */
class RestJson1 : AwsHttpBindingProtocolGenerator() {

    override fun getProtocolHttpBindingResolver(generationContext: ProtocolGenerator.GenerationContext): HttpBindingResolver =
        RestJsonHttpBindingResolver(generationContext)

    override val protocol: ShapeId = RestJson1Trait.ID
}

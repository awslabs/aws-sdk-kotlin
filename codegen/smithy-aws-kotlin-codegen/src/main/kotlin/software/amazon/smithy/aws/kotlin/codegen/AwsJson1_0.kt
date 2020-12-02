/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package software.amazon.smithy.aws.kotlin.codegen

import software.amazon.smithy.aws.traits.protocols.AwsJson1_0Trait
import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.model.shapes.ShapeId

/**
 * Handles generating the `aws.protocols#awsJson1_0` protocol for services.
 *
 * @inheritDoc
 * @see RestJsonProtocolGenerator
 */
class AwsJson1_0 : RestJsonProtocolGenerator() {
    override val defaultContentType: String = "application/json"
    override val protocol: ShapeId = AwsJson1_0Trait.ID
}

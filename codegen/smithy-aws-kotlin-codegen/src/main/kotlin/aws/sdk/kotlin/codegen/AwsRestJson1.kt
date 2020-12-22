/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.aws.traits.protocols.RestJson1Trait
import software.amazon.smithy.model.shapes.ShapeId

/**
 * Handles generating the aws.rest-json protocol for services.
 *
 * @inheritDoc
 * @see RestJsonProtocolGenerator
 */
class AwsRestJson1 : RestJsonProtocolGenerator() {
    override val defaultContentType: String = "application/json"
    override val protocol: ShapeId = RestJson1Trait.ID
}

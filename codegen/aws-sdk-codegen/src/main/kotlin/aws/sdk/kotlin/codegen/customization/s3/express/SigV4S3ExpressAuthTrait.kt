/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AbstractTrait

/**
 * Synthetic auth trait applied to S3's model to enable SigV4 S3 Express auth scheme.
 */
internal class SigV4S3ExpressAuthTrait : AbstractTrait(ID, Node.objectNode()) {
    companion object {
        val ID = ShapeId.from("aws.auth#sigv4s3express")
    }
    override fun createNode(): Node = Node.objectNode()
    override fun isSynthetic(): Boolean = true
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen.customization.s3.express

import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AbstractTrait

/**
 * Synthetic trait representing the `sigv4-s3express` auth scheme.
 */
public class SigV4S3ExpressTrait : AbstractTrait(ID, Node.objectNode()) {
    public companion object {
        val ID = ShapeId.from("com.amazonaws.s3#sigv4express")
        // FIXME What shape ID should be used for sigv4-s3express? It's not in Smithy...
        // Go v2 uses this.  https://github.com/aws/aws-sdk-go-v2/blob/4ba37053fff9055b216a303ab591f6fa5e4c80c1/service/s3/endpoint_auth_resolver.go#L29C1-L29C1
    }

    override fun createNode(): Node = Node.objectNode()

    override fun isSynthetic(): Boolean = true
}

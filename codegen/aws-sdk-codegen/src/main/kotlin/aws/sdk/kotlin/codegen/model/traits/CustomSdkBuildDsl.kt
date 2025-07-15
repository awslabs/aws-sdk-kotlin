package aws.sdk.kotlin.codegen.model.traits

import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AnnotationTrait

class CustomSdkBuildDsl : AnnotationTrait {
    companion object {
        val ID: ShapeId = ShapeId.from("aws.sdk.kotlin.traits#customSdkBuildDsl")
    }

    constructor(node: ObjectNode) : super(ID, node)
    constructor() : this(Node.objectNode())
}

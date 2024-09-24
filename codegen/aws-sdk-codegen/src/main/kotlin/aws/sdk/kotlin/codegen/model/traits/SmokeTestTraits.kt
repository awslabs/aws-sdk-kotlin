package aws.sdk.kotlin.codegen.model.traits

import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.AnnotationTrait

/**
 * Indicates the annotated service should always return a failed response.
 */
class FailedResponseTrait(node: ObjectNode) : AnnotationTrait(ID, node) {
    companion object {
        val ID: ShapeId = ShapeId.from("smithy.kotlin.traits#failedResponseTrait")
    }
}

/**
 * Indicates the annotated service should always return a success response.
 */
class SuccessResponseTrait(node: ObjectNode) : AnnotationTrait(ID, node) {
    companion object {
        val ID: ShapeId = ShapeId.from("smithy.kotlin.traits#successResponseTrait")
    }
}

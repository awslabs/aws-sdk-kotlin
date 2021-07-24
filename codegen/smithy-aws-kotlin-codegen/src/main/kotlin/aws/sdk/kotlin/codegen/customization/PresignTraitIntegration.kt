package aws.sdk.kotlin.codegen.customization

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.traits.Trait
import software.amazon.smithy.model.transform.ModelTransformer

// The specification for which service operations are presignable
internal val presignableOperations: Map<String, Set<String>> = mapOf(
    "com.amazonaws.s3#AmazonS3" to setOf("com.amazonaws.s3#GetObject", "com.amazonaws.s3#PutObject", "com.amazonaws.s3#UploadPart"),
    // FIXME ~ Following operation signature fails service side.
    // "com.amazonaws.sts#AWSSecurityTokenServiceV20110615" to setOf("com.amazonaws.sts#GetCallerIdentity")
)

/**
 * This integration applies a custom trait to any AWS service that provides presign capability on one or more operations.
 */
class PresignTraitIntegration(private val presignOpModel: Map<String, Set<String>> = presignableOperations) : KotlinIntegration {
    /**
     * This custom trait designates operations from which presigners should be generated.
     * This trait may be generalized to Smithy itself.  If this happens, this integration should be removed
     * entirely.
     */
    class PresignTrait : Trait {
        companion object {
            val shapeId: ShapeId = ShapeId.from("aws.sdk#presignable")
        }
        override fun toNode(): Node = ObjectNode(mapOf(), sourceLocation)
        override fun toShapeId(): ShapeId = shapeId
    }

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val currentServiceId = model.expectShape<ServiceShape>(settings.service).id.toString()

        return presignOpModel.keys.contains(currentServiceId)
    }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val currentServiceId = model.expectShape<ServiceShape>(settings.service).id.toString()
        val presignedOperationIds = presignOpModel[currentServiceId]
            ?: error("Expected operation id for service $currentServiceId, but none found in $presignOpModel")
        val transformer = ModelTransformer.create()

        return transformer.mapShapes(model) { shape ->
            if (presignedOperationIds.contains(shape.id.toString())) {
                shape.asOperationShape().get().toBuilder().addTrait(PresignTrait()).build()
            } else {
                shape
            }
        }
    }
}

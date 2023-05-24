package aws.sdk.kotlin.codegen.customization.s3

import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.traits.PaginationTruncationMember
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.MemberShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.transform.ModelTransformer

private val TRUNCATABLE_PAGINATION_OPS = mapOf(
    "com.amazonaws.s3#ListParts" to "IsTruncated",
)

/**
 * Applies the [PaginationTruncationMember] annotation to a manually-curated list of operations and members to handle
 * non-standard pagination termination conditions.
 */
class TruncatablePaginationIntegration : KotlinIntegration {
    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean =
        model.expectShape<ServiceShape>(settings.service).isS3

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val truncatableTargets = model
            .operationShapes
            .mapNotNull { op ->
                TRUNCATABLE_PAGINATION_OPS[op.id.toString()]?.let { member ->
                    model.expectShape<StructureShape>(op.outputShape).allMembers.getValue(member)
                }
            }
            .toSet()

        return ModelTransformer.create().mapShapes(model) { shape ->
            if (shape in truncatableTargets) {
                check(shape is MemberShape) { "Cannot apply PaginationTruncationMember to non-member shape" }
                shape.toBuilder().addTrait(PaginationTruncationMember()).build()
            } else {
                shape
            }
        }
    }
}

package aws.sdk.kotlin.codegen.transforms

import aws.sdk.kotlin.codegen.model.traits.CustomSdkBuildDsl
import software.amazon.smithy.build.ProjectionTransformer
import software.amazon.smithy.build.TransformContext
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.shapes.OperationShape

class AddCustomSdkBuildDslTransformer : ProjectionTransformer {
    override fun getName(): String = "addCustomSdkBuildDsl"

    override fun transform(context: TransformContext): Model? =
        context.transformer.mapShapes(context.model) { shape ->
            when (shape) {
                is OperationShape -> shape.toBuilder().addTrait(CustomSdkBuildDsl()).build()
                else -> shape
            }
        }
}

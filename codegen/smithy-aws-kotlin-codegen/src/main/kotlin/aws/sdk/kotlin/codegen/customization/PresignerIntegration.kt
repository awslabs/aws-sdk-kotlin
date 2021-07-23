package aws.sdk.kotlin.codegen.customization

import aws.sdk.kotlin.codegen.AwsRuntimeTypes
import aws.sdk.kotlin.codegen.protocols.core.EndpointResolverGenerator
import software.amazon.smithy.aws.traits.auth.SigV4Trait
import software.amazon.smithy.codegen.core.Symbol
import software.amazon.smithy.kotlin.codegen.KotlinSettings
import software.amazon.smithy.kotlin.codegen.core.CodegenContext
import software.amazon.smithy.kotlin.codegen.core.DEFAULT_SOURCE_SET_ROOT
import software.amazon.smithy.kotlin.codegen.core.KotlinDelegator
import software.amazon.smithy.kotlin.codegen.core.KotlinWriter
import software.amazon.smithy.kotlin.codegen.core.RenderingContext
import software.amazon.smithy.kotlin.codegen.core.RuntimeTypes
import software.amazon.smithy.kotlin.codegen.core.addImport
import software.amazon.smithy.kotlin.codegen.core.defaultName
import software.amazon.smithy.kotlin.codegen.core.unionVariantName
import software.amazon.smithy.kotlin.codegen.core.withBlock
import software.amazon.smithy.kotlin.codegen.integration.KotlinIntegration
import software.amazon.smithy.kotlin.codegen.lang.KotlinTypes
import software.amazon.smithy.kotlin.codegen.model.buildSymbol
import software.amazon.smithy.kotlin.codegen.model.expectShape
import software.amazon.smithy.kotlin.codegen.model.expectTrait
import software.amazon.smithy.kotlin.codegen.model.hasTrait
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigGenerator
import software.amazon.smithy.kotlin.codegen.rendering.ClientConfigProperty
import software.amazon.smithy.kotlin.codegen.rendering.serde.serializerName
import software.amazon.smithy.kotlin.codegen.utils.namespaceToPath
import software.amazon.smithy.model.Model
import software.amazon.smithy.model.node.Node
import software.amazon.smithy.model.node.NumberNode
import software.amazon.smithy.model.node.ObjectNode
import software.amazon.smithy.model.node.StringNode
import software.amazon.smithy.model.shapes.OperationShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.ShapeId
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.model.traits.Trait
import software.amazon.smithy.model.transform.ModelTransformer
import software.amazon.smithy.utils.MapUtils
import java.util.logging.Logger

/**
 * This integration applies to any AWS service that provides presign capability on one or more operations.
 */
class PresignerIntegration(private val presignOpModel: Set<PresignableOperation> = servicesWithOperationPresigners) : KotlinIntegration {
    private val presignableServiceIds = presignOpModel.map { it.serviceId }.toSet()

    override fun enabledForService(model: Model, settings: KotlinSettings): Boolean {
        val currentServiceId = model.expectShape<ServiceShape>(settings.service).id.toString()

        return presignableServiceIds.contains(currentServiceId)
    }

    class PresignTrait : Trait {
        companion object {
            val shapeId: ShapeId = ShapeId.from("aws.sdk#Presignable")
        }
        override fun toNode(): Node = ObjectNode(mapOf(), sourceLocation)
        override fun toShapeId(): ShapeId = shapeId
    }

    override fun preprocessModel(model: Model, settings: KotlinSettings): Model {
        val presignedOperationIds = presignOpModel.map { presignableOperation -> presignableOperation.operationId }
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

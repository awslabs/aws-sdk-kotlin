package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.codegen.model.Types

/**
 * A container object for various DynamoDbMapper [Type] instances
 */
internal object MapperTypes {
    // Low-level types
    val AttributeValue = TypeRef(MapperPkg.Ll.Model, "AttributeValue")
    val AttributeMap = Types.Kotlin.map(Types.Kotlin.String, AttributeValue)

    // High-level types
    val DynamoDbMapper = TypeRef(MapperPkg.Hl.Base, "DynamoDbMapper")

    object Annotations {
        val ManualPagination = TypeRef(MapperPkg.Hl.Annotations, "ManualPagination")
    }

    object Items {
        fun itemSchema(typeVar: String) =
            TypeRef(MapperPkg.Hl.Items, "ItemSchema", genericArgs = listOf(TypeVar(typeVar)))

        fun itemSchemaPartitionKey(objectType: TypeRef, pkType: TypeRef) =
            TypeRef(MapperPkg.Hl.Items, "ItemSchema.PartitionKey", listOf(objectType, pkType))

        fun itemSchemaCompositeKey(objectType: TypeRef, pkType: TypeRef, skType: TypeRef) =
            TypeRef(MapperPkg.Hl.Items, "ItemSchema.CompositeKey", listOf(objectType, pkType, skType))

        fun keySpec(keyType: TypeRef) = TypeRef(MapperPkg.Hl.Items, "KeySpec", genericArgs = listOf(keyType))
        val KeySpecNumber = TypeRef(MapperPkg.Hl.Items, "KeySpec.Number")
        val KeySpecString = TypeRef(MapperPkg.Hl.Items, "KeySpec.String")
        val AttributeDescriptor = TypeRef(MapperPkg.Hl.Items, "AttributeDescriptor")

        fun itemConverter(objectType: TypeRef) =
            TypeRef(MapperPkg.Hl.Items, "ItemConverter", genericArgs = listOf(objectType))

        val SimpleItemConverter = TypeRef(MapperPkg.Hl.Items, "SimpleItemConverter")
    }

    object Model {
        fun tablePartitionKey(objectType: TypeRef, pkType: TypeRef) = TypeRef(
            MapperPkg.Hl.Model,
            "Table.PartitionKey",
            genericArgs = listOf(objectType, pkType),
        )
        fun tableCompositeKey(objectType: TypeRef, pkType: TypeRef, skType: TypeRef) = TypeRef(
            MapperPkg.Hl.Model,
            "Table.CompositeKey",
            genericArgs = listOf(objectType, pkType, skType),
        )
        val toItem = TypeRef(MapperPkg.Hl.Model, "toItem")
    }

    object Values {
        object Scalars {
            val BooleanConverter = TypeRef(MapperPkg.Hl.ScalarValues, "BooleanConverter")
            val IntConverter = TypeRef(MapperPkg.Hl.ScalarValues, "IntConverter")
            val StringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "StringConverter")
        }

        object SmithyTypes {
            val DefaultInstantConverter = TypeRef(MapperPkg.Hl.SmithyTypeValues, "InstantConverters.Default")
        }
    }

    object PipelineImpl {
        val HReqContextImpl = TypeRef(MapperPkg.Hl.PipelineImpl, "HReqContextImpl")
        val MapperContextImpl = TypeRef(MapperPkg.Hl.PipelineImpl, "MapperContextImpl")
        val Operation = TypeRef(MapperPkg.Hl.PipelineImpl, "Operation")
    }
}

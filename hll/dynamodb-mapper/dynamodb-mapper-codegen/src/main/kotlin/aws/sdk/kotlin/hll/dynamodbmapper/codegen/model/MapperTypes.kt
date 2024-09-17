package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.codegen.model.Pkg
import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.codegen.model.Types

/**
 * A container object for various DynamoDbMapper [Type] instances
 */
internal object MapperTypes {
    // Low-level types
    val AttributeValue = TypeRef(Pkg.Ll.Model, "AttributeValue")
    val AttributeMap = Types.Kotlin.map(Types.Kotlin.String, AttributeValue)

    // High-level types
    val DynamoDbMapper = TypeRef(Pkg.Hl.Base, "DynamoDbMapper")

    object Annotations {
        val ManualPagination = TypeRef(Pkg.Hl.Annotations, "ManualPagination")
    }

    object Items {
        fun itemSchema(typeVar: String) = TypeRef(Pkg.Hl.Items, "ItemSchema", listOf(TypeVar(typeVar)))
        fun itemSchemaPartitionKey(objectType: TypeRef, pkType: TypeRef) = TypeRef(Pkg.Hl.Items, "ItemSchema.PartitionKey", listOf(objectType, pkType))
        fun itemSchemaCompositeKey(objectType: TypeRef, pkType: TypeRef, skType: TypeRef) = TypeRef(Pkg.Hl.Items, "ItemSchema.CompositeKey", listOf(objectType, pkType, skType))
        fun keySpec(keyType: TypeRef) = TypeRef(Pkg.Hl.Items, "KeySpec", genericArgs = listOf(keyType))
        val KeySpecNumber = TypeRef(Pkg.Hl.Items, "KeySpec.Number")
        val KeySpecString = TypeRef(Pkg.Hl.Items, "KeySpec.String")
        val AttributeDescriptor = TypeRef(Pkg.Hl.Items, "AttributeDescriptor")
        fun itemConverter(objectType: TypeRef) = TypeRef(Pkg.Hl.Items, "ItemConverter", genericArgs = listOf(objectType))
        val SimpleItemConverter = TypeRef(Pkg.Hl.Items, "SimpleItemConverter")
    }

    object Model {
        fun tablePartitionKey(objectType: TypeRef, pkType: TypeRef) = TypeRef(
            Pkg.Hl.Model,
            "Table.PartitionKey",
            genericArgs = listOf(objectType, pkType),
        )
        fun tableCompositeKey(objectType: TypeRef, pkType: TypeRef, skType: TypeRef) = TypeRef(
            Pkg.Hl.Model,
            "Table.CompositeKey",
            genericArgs = listOf(objectType, pkType, skType),
        )
        val toItem = TypeRef(Pkg.Hl.Model, "toItem")
    }

    object Values {
        object Scalars {
            val BooleanConverter = TypeRef(Pkg.Hl.ScalarValues, "BooleanConverter")
            val IntConverter = TypeRef(Pkg.Hl.ScalarValues, "IntConverter")
            val StringConverter = TypeRef(Pkg.Hl.ScalarValues, "StringConverter")
        }

        object SmithyTypes {
            val DefaultInstantConverter = TypeRef(Pkg.Hl.SmithyTypeValues, "InstantConverters.Default")
        }
    }

    object PipelineImpl {
        val HReqContextImpl = TypeRef(Pkg.Hl.PipelineImpl, "HReqContextImpl")
        val MapperContextImpl = TypeRef(Pkg.Hl.PipelineImpl, "MapperContextImpl")
        val Operation = TypeRef(Pkg.Hl.PipelineImpl, "Operation")
    }
}

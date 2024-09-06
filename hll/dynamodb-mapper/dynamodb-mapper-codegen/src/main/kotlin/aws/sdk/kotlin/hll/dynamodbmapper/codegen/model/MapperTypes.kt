package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.util.Pkg

/**
 * A container object for various DynamoDbMapper [Type] instances
 */
internal object MapperTypes {
    // Low-level types
    val AttributeValue = TypeRef(Pkg.Ll.Model, "AttributeValue")
    val AttributeMap = Types.Kotlin.map(Types.Kotlin.String, AttributeValue)

    // High-level types
    val DynamoDbMapper = TypeRef(Pkg.Hl.Base, "DynamoDbMapper")

    object Items {
        fun itemSchema(typeVar: String) = TypeRef(Pkg.Hl.Items, "ItemSchema", listOf(TypeVar(typeVar)))
        fun itemSchemaPartitionKey(objectType: TypeRef, pkType: TypeRef) = TypeRef(Pkg.Hl.Items, "ItemSchema.PartitionKey", listOf(objectType, pkType))
        fun itemSchemaCompositeKey(objectType: TypeRef, pkType: TypeRef, skType: TypeRef) = TypeRef(Pkg.Hl.Items, "ItemSchema.CompositeKey", listOf(objectType, pkType, skType))
        fun keySpec(keyType: TypeRef) = TypeRef(Pkg.Hl.Items, "KeySpec", genericArgs = listOf(keyType))
        val KeySpecNumber = TypeRef(Pkg.Hl.Items, "KeySpec.Number")
        val KeySpecString = TypeRef(Pkg.Hl.Items, "KeySpec.String")
        val KeySpecByteArray = TypeRef(Pkg.Hl.Items, "KeySpec.ByteArray")
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
        object Collections {
            fun listConverter(elementConverter: TypeRef) = TypeRef(Pkg.Hl.CollectionValues, "ListConverter", genericArgs = listOf(elementConverter))

            val StringSetConverter = TypeRef(Pkg.Hl.CollectionValues, "StringSetConverter")
            val CharSetConverter = TypeRef(Pkg.Hl.CollectionValues, "CharSetConverter")
            val CharArraySetConverter = TypeRef(Pkg.Hl.CollectionValues, "CharArraySetConverter")

            val ByteSetConverter = TypeRef(Pkg.Hl.CollectionValues, "ByteSetConverter")
            val DoubleSetConverter = TypeRef(Pkg.Hl.CollectionValues, "DoubleSetConverter")
            val FloatSetConverter = TypeRef(Pkg.Hl.CollectionValues, "FloatSetConverter")
            val IntSetConverter = TypeRef(Pkg.Hl.CollectionValues, "IntSetConverter")
            val LongSetConverter = TypeRef(Pkg.Hl.CollectionValues, "LongSetConverter")
            val ShortSetConverter = TypeRef(Pkg.Hl.CollectionValues, "ShortSetConverter")

            val UByteSetConverter = TypeRef(Pkg.Hl.CollectionValues, "UByteSetConverter")
            val UIntSetConverter = TypeRef(Pkg.Hl.CollectionValues, "UIntSetConverter")
            val ULongSetConverter = TypeRef(Pkg.Hl.CollectionValues, "ULongSetConverter")
            val UShortSetConverter = TypeRef(Pkg.Hl.CollectionValues, "UShortSetConverter")
        }

        object Scalars {
            val BooleanConverter = TypeRef(Pkg.Hl.ScalarValues, "BooleanConverter")
            val StringConverter = TypeRef(Pkg.Hl.ScalarValues, "StringConverter")
            val CharConverter = TypeRef(Pkg.Hl.ScalarValues, "CharConverter")
            val CharArrayConverter = TypeRef(Pkg.Hl.ScalarValues, "CharArrayConverter")

            val ByteConverter = TypeRef(Pkg.Hl.ScalarValues, "ByteConverter")
            val ByteArrayConverter = TypeRef(Pkg.Hl.ScalarValues, "ByteArrayConverter")
            val DoubleConverter = TypeRef(Pkg.Hl.ScalarValues, "DoubleConverter")
            val FloatConverter = TypeRef(Pkg.Hl.ScalarValues, "FloatConverter")
            val IntConverter = TypeRef(Pkg.Hl.ScalarValues, "IntConverter")
            val LongConverter = TypeRef(Pkg.Hl.ScalarValues, "LongConverter")
            val ShortConverter = TypeRef(Pkg.Hl.ScalarValues, "ShortConverter")
            val UByteConverter = TypeRef(Pkg.Hl.ScalarValues, "UByteConverter")
            val UIntConverter = TypeRef(Pkg.Hl.ScalarValues, "UIntConverter")
            val ULongConverter = TypeRef(Pkg.Hl.ScalarValues, "ULongConverter")
            val UShortConverter = TypeRef(Pkg.Hl.ScalarValues, "UShortConverter")
        }

        object SmithyTypes {
            val DefaultInstantConverter = TypeRef(Pkg.Hl.SmithyTypeValues, "InstantConverter.Default")
            val UrlConverter = TypeRef(Pkg.Hl.SmithyTypeValues, "UrlConverter")
            val DefaultDocumentConverter = TypeRef(Pkg.Hl.SmithyTypeValues, "DocumentConverter.Default")
        }
    }

    object PipelineImpl {
        val HReqContextImpl = TypeRef(Pkg.Hl.PipelineImpl, "HReqContextImpl")
        val MapperContextImpl = TypeRef(Pkg.Hl.PipelineImpl, "MapperContextImpl")
        val Operation = TypeRef(Pkg.Hl.PipelineImpl, "Operation")
    }
}

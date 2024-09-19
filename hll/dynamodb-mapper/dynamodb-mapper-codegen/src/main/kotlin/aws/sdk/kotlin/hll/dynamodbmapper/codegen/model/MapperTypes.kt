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
            TypeRef(MapperPkg.Hl.Items, "ItemSchema.PartitionKey", genericArgs = listOf(objectType, pkType))

        fun itemSchemaCompositeKey(objectType: TypeRef, pkType: TypeRef, skType: TypeRef) =
            TypeRef(MapperPkg.Hl.Items, "ItemSchema.CompositeKey", genericArgs = listOf(objectType, pkType, skType))

        fun keySpec(keyType: TypeRef) = TypeRef(MapperPkg.Hl.Items, "KeySpec", genericArgs = listOf(keyType))
        val KeySpecByteArray = TypeRef(MapperPkg.Hl.Items, "KeySpec.ByteArray")
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
        fun valueConverter(value: Type) = TypeRef(MapperPkg.Hl.Values, "ValueConverter", genericArgs = listOf(value))
        val ItemToValueConverter = TypeRef(MapperPkg.Hl.Values, "ItemToValueConverter")

        object Collections {
            val ListConverter = TypeRef(MapperPkg.Hl.CollectionValues, "ListConverter")
            val MapConverter = TypeRef(MapperPkg.Hl.CollectionValues, "MapConverter")

            val StringSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "StringSetConverter")
            val CharSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "CharSetConverter")
            val CharArraySetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "CharArraySetConverter")

            val ByteSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "ByteSetConverter")
            val DoubleSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "DoubleSetConverter")
            val FloatSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "FloatSetConverter")
            val IntSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "IntSetConverter")
            val LongSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "LongSetConverter")
            val ShortSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "ShortSetConverter")

            val UByteSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "UByteSetConverter")
            val UIntSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "UIntSetConverter")
            val ULongSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "ULongSetConverter")
            val UShortSetConverter = TypeRef(MapperPkg.Hl.CollectionValues, "UShortSetConverter")
        }

        object Scalars {
            fun enumConverter(enumType: Type) = TypeRef(MapperPkg.Hl.ScalarValues, "EnumConverter", genericArgs = listOf(enumType))

            val BooleanConverter = TypeRef(MapperPkg.Hl.ScalarValues, "BooleanConverter")
            val StringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "StringConverter")
            val CharConverter = TypeRef(MapperPkg.Hl.ScalarValues, "CharConverter")
            val CharArrayConverter = TypeRef(MapperPkg.Hl.ScalarValues, "CharArrayConverter")

            val ByteConverter = TypeRef(MapperPkg.Hl.ScalarValues, "ByteConverter")
            val ByteArrayConverter = TypeRef(MapperPkg.Hl.ScalarValues, "ByteArrayConverter")
            val DoubleConverter = TypeRef(MapperPkg.Hl.ScalarValues, "DoubleConverter")
            val FloatConverter = TypeRef(MapperPkg.Hl.ScalarValues, "FloatConverter")
            val IntConverter = TypeRef(MapperPkg.Hl.ScalarValues, "IntConverter")
            val LongConverter = TypeRef(MapperPkg.Hl.ScalarValues, "LongConverter")
            val ShortConverter = TypeRef(MapperPkg.Hl.ScalarValues, "ShortConverter")
            val UByteConverter = TypeRef(MapperPkg.Hl.ScalarValues, "UByteConverter")
            val UIntConverter = TypeRef(MapperPkg.Hl.ScalarValues, "UIntConverter")
            val ULongConverter = TypeRef(MapperPkg.Hl.ScalarValues, "ULongConverter")
            val UShortConverter = TypeRef(MapperPkg.Hl.ScalarValues, "UShortConverter")

            val BooleanToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "BooleanToStringConverter")
            val CharArrayToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "TextConverters.CharArrayToStringConverter")
            val CharToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "TextConverters.CharToStringConverter")
            val StringToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "TextConverters.StringToStringConverter")
            val ByteToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.ByteToStringConverter")
            val DoubleToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.DoubleToStringConverter")
            val FloatToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.FloatToStringConverter")
            val IntToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.IntToStringConverter")
            val LongToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.LongToStringConverter")
            val ShortToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.ShortToStringConverter")
            val UByteToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.UByteToStringConverter")
            val UIntToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.UIntToStringConverter")
            val ULongToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.ULongToStringConverter")
            val UShortToStringConverter = TypeRef(MapperPkg.Hl.ScalarValues, "NumberConverters.UShortToStringConverter")
        }

        object SmithyTypes {
            val DefaultInstantConverter = TypeRef(MapperPkg.Hl.SmithyTypeValues, "InstantConverter.Default")
            val UrlConverter = TypeRef(MapperPkg.Hl.SmithyTypeValues, "UrlConverter")
            val DefaultDocumentConverter = TypeRef(MapperPkg.Hl.SmithyTypeValues, "DocumentConverter.Default")
        }
    }

    object PipelineImpl {
        val HReqContextImpl = TypeRef(MapperPkg.Hl.PipelineImpl, "HReqContextImpl")
        val MapperContextImpl = TypeRef(MapperPkg.Hl.PipelineImpl, "MapperContextImpl")
        val Operation = TypeRef(MapperPkg.Hl.PipelineImpl, "Operation")
    }
}

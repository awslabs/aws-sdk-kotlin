package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.util.Pkg

/**
 * A container object for various DynamoDbMapper [Type] instances
 */
public object MapperTypes {
    // Low-level types
    public val AttributeValue: TypeRef = TypeRef(Pkg.Ll.Model, "AttributeValue")
    public val AttributeMap: TypeRef = Types.Kotlin.map(Types.Kotlin.String, AttributeValue)

    // High-level types
    public val DynamoDbMapper: TypeRef = TypeRef(Pkg.Hl.Base, "DynamoDbMapper")

    public object Items {
        public fun itemSchema(typeVar: String): TypeRef = TypeRef(Pkg.Hl.Items, "ItemSchema", listOf(TypeVar(typeVar)))
        public fun itemSchemaPartitionKey(objectType: TypeRef, keyType: TypeRef): TypeRef = TypeRef(Pkg.Hl.Items, "ItemSchema.PartitionKey", listOf(objectType, keyType))
        public fun keySpec(keyType: TypeRef): TypeRef = TypeRef(Pkg.Hl.Items, "KeySpec", genericArgs = listOf(keyType))
        public val KeySpecNumber: TypeRef = TypeRef(Pkg.Hl.Items, "KeySpec.Number")
        public val KeySpecString: TypeRef = TypeRef(Pkg.Hl.Items, "KeySpec.String")
        public val AttributeDescriptor: TypeRef = TypeRef(Pkg.Hl.Items, "AttributeDescriptor")
        public fun itemConverter(objectType: TypeRef): TypeRef = TypeRef(Pkg.Hl.Items, "ItemConverter", genericArgs = listOf(objectType))
        public val SimpleItemConverter: TypeRef = TypeRef(Pkg.Hl.Items, "SimpleItemConverter")
    }

    public object Model {
        public fun tablePartitionKey(objectType: TypeRef, keyType: TypeRef): TypeRef = TypeRef(
            Pkg.Hl.Model,
            "Table.PartitionKey",
            genericArgs = listOf(objectType, keyType),
        )
        public val toItem: TypeRef = TypeRef(Pkg.Hl.Model, "toItem")
    }

    public object Values {
        public val DefaultInstantConverter: TypeRef = TypeRef(Pkg.Hl.Values, "InstantConverter.Default")
        public val BooleanConverter: TypeRef = TypeRef(Pkg.Hl.Values, "BooleanConverter")
        public val IntConverter: TypeRef = TypeRef(Pkg.Hl.Values, "IntConverter")
        public val StringConverter: TypeRef = TypeRef(Pkg.Hl.Values, "StringConverter")
    }

    public object PipelineImpl {
        public val HReqContextImpl: TypeRef = TypeRef(Pkg.Hl.PipelineImpl, "HReqContextImpl")
        public val MapperContextImpl: TypeRef = TypeRef(Pkg.Hl.PipelineImpl, "MapperContextImpl")
        public val Operation: TypeRef = TypeRef(Pkg.Hl.PipelineImpl, "Operation")
    }
}

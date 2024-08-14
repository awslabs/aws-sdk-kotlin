package aws.sdk.kotlin.hll.dynamodbmapper.codegen.model

import aws.sdk.kotlin.hll.codegen.model.Type
import aws.sdk.kotlin.hll.codegen.model.TypeRef
import aws.sdk.kotlin.hll.codegen.model.TypeVar
import aws.sdk.kotlin.hll.codegen.model.Types
import aws.sdk.kotlin.hll.codegen.util.Pkg

/**
 * A container object for various DynamoDbMapper [Type] instances
 */
object DynamoDbMapperTypes {
    // Low-level types
    val AttributeValue = TypeRef(Pkg.Ll.Model, "AttributeValue")
    val AttributeMap = Types.Kotlin.map(Types.Kotlin.String, AttributeValue)

    // High-level types
    val DynamoDbMapper = TypeRef(Pkg.Hl.Base, "DynamoDbMapper")

    val HReqContextImpl = TypeRef(Pkg.Hl.PipelineImpl, "HReqContextImpl")
    fun itemSchema(typeVar: String) = TypeRef(Pkg.Hl.Items, "ItemSchema", listOf(TypeVar(typeVar)))
    val MapperContextImpl = TypeRef(Pkg.Hl.PipelineImpl, "MapperContextImpl")
    val Operation = TypeRef(Pkg.Hl.PipelineImpl, "Operation")

    fun tablePartitionKey(objectType: TypeRef, partitionKeyType: TypeRef) = TypeRef(
        Pkg.Hl.Model,
        "Table.PartitionKey",
        genericArgs = listOf(objectType, partitionKeyType),
    )
    val toItem = TypeRef(Pkg.Hl.Model, "toItem")

    val KeySpec = TypeRef(Pkg.Hl.Items, "KeySpec")
    val ItemSchema = TypeRef(Pkg.Hl.Items, "ItemSchema")
    val AttributeDescriptor = TypeRef(Pkg.Hl.Items, "AttributeDescriptor")
    val ItemConverter = TypeRef(Pkg.Hl.Items, "ItemConverter")
    val SimpleItemConverter = TypeRef(Pkg.Hl.Items, "SimpleItemConverter")

    val DefaultInstantConverter = TypeRef(Pkg.Hl.Values, "InstantConverter.Default")
    val BooleanConverter = TypeRef(Pkg.Hl.Values, "BooleanConverter")
    val IntConverter = TypeRef(Pkg.Hl.Values, "IntConverter")
    val StringConverter = TypeRef(Pkg.Hl.Values, "StringConverter")
}

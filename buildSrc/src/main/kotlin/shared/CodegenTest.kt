package shared

/**
 * An AWS SDK for Kotlin codegen test
 */
data class CodegenTest(
    val name: String,
    val model: Model,
    val serviceShapeId: String,
    val protocolName: String? = null,
)

/**
 * A smithy model file
 */
data class Model(
    val fileName: String,
    val path: String = "src/commonTest/resources/",
)

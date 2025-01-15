package aws.sdk.kotlin.tests.codegen

/**
 * An AWS SDK for Kotlin codegen test
 */
data class CodegenTest(
    val name: String,
    val model: Model,
    val serviceShapeId: String,
)

/**
 * A smithy model file
 */
data class Model(
    val fileName: String,
    val path: String = "src/test/resources/",
)

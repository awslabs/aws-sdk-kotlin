package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.Visibility.IMPLICIT
import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Options for configuring annotation processor codegen
 */
object AnnotationsProcessorOptions {
    /**
     * Determines when a builder class should be generated for user classes. Defaults to [GenerateBuilderClasses.WHEN_REQUIRED].
     * With this setting, builder classes will not be generated for user classes which consist of only public mutable members
     * and have a zero-arg constructor.
     */
    val GenerateBuilderClassesAttribute: AttributeKey<GenerateBuilderClasses> = AttributeKey("GenerateBuilderClasses")

    /**
     * Determines the visibility of code-generated classes / objects. Defaults to [Visibility.IMPLICIT].
     */
    val VisibilityAttribute: AttributeKey<Visibility> = AttributeKey("Visibility")

    /**
     * Determines the package where code-generated classes / objects will be placed.
     * Defaults to [DestinationPackage.RELATIVE] from the package of the class being processed, suffixed with "mapper.schemas".
     */
    val DestinationPackageAttribute: AttributeKey<DestinationPackage> = AttributeKey("DestinationPackage")

    /**
     * Determines whether a `DynamoDbMapper.get<CLASS>Table` convenience extension function will be generated. Defaults to true.
     */
    val GenerateGetTableMethodAttribute: AttributeKey<Boolean> = AttributeKey("GenerateGetTableMethod")
}

/**
 * Determines when a builder class should be generated for user classes. Defaults to "WHEN_REQUIRED".
 * With this setting, builder classes will not be generated for user classes which consist of only public mutable members
 * and have a zero-arg constructor.
 */
enum class GenerateBuilderClasses {
    /**
     * Builders will only be generated when required (the class has non-mutable members / does not have a zero-arg constructor)
     */
    WHEN_REQUIRED,

    /**
     * Builders will always be generated
     */
    ALWAYS,
}

/**
 * Determines the visibility of code-generated classes / objects. Defaults to [IMPLICIT].
 */
enum class Visibility {
    /**
     * An implicit visibility will be used, which is recommended for most use-cases.
     */
    IMPLICIT,

    /**
     * All code-generated constructs will be `public`
     */
    PUBLIC,

    /**
     * All code-generated constructs will be `internal`
     */
    INTERNAL,
}

/**
 * Determines the package where code-generated classes / objects will be placed.
 */
sealed class DestinationPackage {
    abstract val pkg: String
    class RELATIVE(override val pkg: String = "mapper.schemas") : DestinationPackage()
    class ABSOLUTE(override val pkg: String = "") : DestinationPackage()
}

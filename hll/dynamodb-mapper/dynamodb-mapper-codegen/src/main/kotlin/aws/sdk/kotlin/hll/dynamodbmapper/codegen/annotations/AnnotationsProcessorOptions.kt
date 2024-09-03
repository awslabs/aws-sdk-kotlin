package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.Visibility.DEFAULT
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
     * Determines the visibility of code-generated classes / objects. Defaults to [Visibility.DEFAULT].
     */
    val VisibilityAttribute: AttributeKey<Visibility> = AttributeKey("Visibility")

    /**
     * Determines the package where code-generated classes / objects will be placed.
     * Defaults to [DestinationPackage.Relative] from the package of the class being processed, suffixed with `aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas`.
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
 * Determines the visibility of code-generated classes / objects. Defaults to [DEFAULT].
 */
enum class Visibility {
    /**
     * A default, unspecified visibility will be used, which is recommended for most use-cases.
     */
    DEFAULT,

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
 * Defaults to [DestinationPackage.Relative] from the package of the class being processed, suffixed with `aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas`.
 */
sealed class DestinationPackage {
    /**
     * The package where code-generated classes / objects will be placed.
     */
    abstract val pkg: String

    /**
     * Constructs should be code-generated into a package relative to the class being processed. Defaults to `aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas`.
     */
    class Relative(override val pkg: String = "aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas") : DestinationPackage()

    /**
     * Constructs should be code-generated into an absolute package. Defaults to `aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas`.
     */
    class Absolute(override val pkg: String = "aws.sdk.kotlin.hll.dynamodbmapper.generatedschemas") : DestinationPackage()
}

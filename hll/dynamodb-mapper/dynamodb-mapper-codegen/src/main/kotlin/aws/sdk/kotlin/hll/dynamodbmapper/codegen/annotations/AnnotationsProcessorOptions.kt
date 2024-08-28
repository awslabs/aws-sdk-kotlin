import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Options for configuring annotation processor codegen
 */
object AnnotationsProcessorOptions {
    /**
     * Determines when a builder class should be generated for user classes. Defaults to "WHEN_REQUIRED".
     * With this setting, builder classes will not be generated for user classes which consist of only public mutable members
     * and have a zero-arg constructor.
     */
    val GenerateBuilderClasses: AttributeKey<GenerateBuilderClasses> = AttributeKey("GenerateBuilderClasses")
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

package aws.sdk.kotlin.hll.dynamodbmapper.plugins

const val SCHEMA_GENERATOR_PLUGIN_EXTENSION = "schemaGeneratorPluginExtension"

open class SchemaGeneratorPluginExtension {
    /**
     * Determines when a builder class should be generated for user classes. Defaults to "WHEN_REQUIRED".
     * With this setting, builder classes will not be generated for user classes consist of only public mutable members.
     */
    var generateBuilderClasses = GenerateBuilderClasses.WHEN_REQUIRED
}

enum class GenerateBuilderClasses {
    WHEN_REQUIRED,
    ALWAYS,
}

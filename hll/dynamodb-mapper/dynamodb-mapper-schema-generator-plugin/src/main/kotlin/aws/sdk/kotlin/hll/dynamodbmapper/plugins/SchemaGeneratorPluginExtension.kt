package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses

const val SCHEMA_GENERATOR_PLUGIN_EXTENSION = "dynamoDbMapper"

open class SchemaGeneratorPluginExtension {
    /**
     * Determines when a builder class should be generated for user classes. Defaults to "WHEN_REQUIRED".
     * With this setting, builder classes will not be generated for user classes which consist of only public mutable members
     * and have a zero-arg constructor.
     */
    var generateBuilderClasses = GenerateBuilderClasses.WHEN_REQUIRED
}

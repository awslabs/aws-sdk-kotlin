package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.Visibility
import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.Visibility.IMPLICIT

const val SCHEMA_GENERATOR_PLUGIN_EXTENSION = "dynamoDbMapper"

open class SchemaGeneratorPluginExtension {
    /**
     * Determines when a builder class should be generated for user classes. Defaults to "WHEN_REQUIRED".
     * With this setting, builder classes will not be generated for user classes which consist of only public mutable members
     * and have a zero-arg constructor.
     */
    var generateBuilderClasses: GenerateBuilderClasses = GenerateBuilderClasses.WHEN_REQUIRED

    /**
     * Determines the visibility of code-generated classes / objects. Defaults to [IMPLICIT].
     */
    var visibility: Visibility = Visibility.IMPLICIT

    /**
     * Determines the package where code-generated classes / objects will be placed.
     * Defaults to [DestinationPackage.RELATIVE] from the package of the class being processed, suffixed with "mapper.schemas".
     */
    var destinationPackage: DestinationPackage = DestinationPackage.RELATIVE()

    /**
     * Determines whether a `DynamoDbMapper.get<CLASS>Table` convenience extension function will be generated. Defaults to true.
     */
    var generateGetTableExtension: Boolean = true
}

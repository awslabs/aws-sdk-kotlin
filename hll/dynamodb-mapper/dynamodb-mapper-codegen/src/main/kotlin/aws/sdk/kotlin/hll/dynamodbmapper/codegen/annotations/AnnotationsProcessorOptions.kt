package aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations

import aws.smithy.kotlin.runtime.collections.AttributeKey

/**
 * Options for configuring annotation processor codegen
 */
    /**
     * Determines when a builder class should be generated for user classes. Defaults to [GenerateBuilderClasses.WHEN_REQUIRED].
     * With this setting, builder classes will not be generated for user classes which consist of only public mutable members
     * and have a zero-arg constructor.
     */

    /**
     * Determines the package where code-generated classes / objects will be placed.
     */

    /**
     * Determines whether a `DynamoDbMapper.get<CLASS>Table` convenience extension function will be generated. Defaults to true.
     */
}

/**
 * Determines when a builder class should be generated for user classes. Defaults to "WHEN_REQUIRED".
 * With this setting, builder classes will not be generated for user classes which consist of only public mutable members
 * and have a zero-arg constructor.
 */
    /**
     */
    WHEN_REQUIRED,

    /**
     * Builders will always be generated
     */
    ALWAYS,
}

/**
 * Determines the package where code-generated classes / objects will be placed.
 */

    /**
     */

    /**
     */
}

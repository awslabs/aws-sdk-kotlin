package com.amazonaws.service.lambda.model

/**
 * This is a hypothetical type that is not modeled in the Lambda service.  Rather,
 * it's used to demonstrate the Union type in Smithy, for which there is not broad
 * service support at the time of writing this file.
 *
 * If/when there is a Union type modeled in Lambda, this file should be replaced
 * with the actual type(s).
 */
sealed class AliasType {
    class ExpiringAliasType(val value: Long?) : AliasType()
    class RemoteAliasType(val value: String?) : AliasType()
    class MultiAliasType(val value: List<String>) : AliasType()
}
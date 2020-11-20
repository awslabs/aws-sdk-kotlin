/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.amazonaws.service.lambda.model

/**
 * This is a hypothetical type that is not modeled in the Lambda service.  Rather,
 * it's used to demonstrate the Union type in Smithy, for which there is not broad
 * service support at the time of writing this file.
 *
 * If/when there is a Union type modeled in Lambda, this file should be replaced
 * with the actual type(s).
 */
class CreateAliasRequest2 private constructor(builder: BuilderImpl){
    val description: String? = builder.description
    val functionName: String? = builder.functionName
    val functionVersion: String? = builder.functionVersion
    val name: String? = builder.name
    val routingConfig: MutableMap<String, Float>? = builder.routingConfig
    val aliasType: AliasType? = builder.aliasType

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
    }

    interface Builder {
        fun build(): CreateAliasRequest2
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        // Location: PAYLAD; Name: "Description"
        var description: String?

        // Location: URI; Name: "FunctionName"
        var functionName: String?

        // Location: Payload; Name: "FunctionVersion"
        var functionVersion: String?

        // Location: Payload; Name: "Name"
        var name: String?

        // Location: Payload; Name: "RoutingConfig"
        var routingConfig: MutableMap<String, Float>?

        var aliasType: AliasType?
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var description: String? = null
        override var functionName: String? = null
        override var functionVersion: String? = null
        override var name: String? = null
        override var routingConfig: MutableMap<String, Float>? = null
        override var aliasType: AliasType? = null

        override fun build(): CreateAliasRequest2 = CreateAliasRequest2(this)
    }
}

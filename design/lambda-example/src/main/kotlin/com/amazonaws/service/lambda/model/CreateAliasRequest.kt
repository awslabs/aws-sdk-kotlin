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


class CreateAliasRequest private constructor(builder: BuilderImpl){
    val description: String? = builder.description
    val functionName: String? = builder.functionName
    val functionVersion: String? = builder.functionVersion
    val name: String? = builder.name
    val routingConfig: MutableMap<String, Float>? = builder.routingConfig

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
    }

    interface Builder {
        fun build(): CreateAliasRequest
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
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var description: String? = null
        override var functionName: String? = null
        override var functionVersion: String? = null
        override var name: String? = null
        override var routingConfig: MutableMap<String, Float>? = null

        override fun build(): CreateAliasRequest = CreateAliasRequest(this)
    }
}


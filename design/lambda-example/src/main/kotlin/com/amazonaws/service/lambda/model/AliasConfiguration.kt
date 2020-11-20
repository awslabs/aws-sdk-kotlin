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


class AliasConfiguration private constructor(builder: BuilderImpl){
    val aliasArn: String? = builder.aliasArn
    val description: String? = builder.description
    val functionVersion: String? = builder.functionVersion
    val name: String? = builder.name
    val revisionId: String? = builder.revisionId
    val routingConfig: AliasRoutingConfiguration? = builder.routingConfig

    // TODO - not real implementation, for debug only right now
    override fun toString(): String = buildString {
        appendln("AliasConfiguration(")
        appendln("\taliasArn: $aliasArn")
        appendln("\tdescription: $description")
        appendln("\tfunctionVersion: $functionVersion")
        appendln("\tname: $name")
        appendln("\trevisionId: $revisionId")
        appendln("\troutingConfig: $routingConfig")
        appendln(")")
    }

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
        fun dslBuilder(): DslBuilder = BuilderImpl()
    }

    interface Builder {
        fun build(): AliasConfiguration
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        // Location: Payload; Name: "AliasArn"
        var aliasArn: String?

        // Location: Payload; Name: "Description"
        var description: String?

        // Location: Payload; Name: "FunctionVersion"
        var functionVersion: String?

        // Location: Payload; Name: "Name"
        var name: String?

        // Location: Payload; Name: "RevisionId"
        var revisionId: String?

        // Location: Payload; Name: "RoutingConfig"
        var routingConfig: AliasRoutingConfiguration?

        fun routingConfig(block: AliasRoutingConfiguration.DslBuilder.() -> Unit) = AliasRoutingConfiguration.invoke(block)

        fun build(): AliasConfiguration
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var aliasArn: String? = null
        override var description: String? = null
        override var functionVersion: String? = null
        override var name: String? = null
        override var revisionId: String? = null
        override var routingConfig: AliasRoutingConfiguration? = null

        override fun build(): AliasConfiguration = AliasConfiguration(this)
    }
}




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


class AliasRoutingConfiguration private constructor(builder: BuilderImpl){
    val additionalVersionWeights: Map<String, Float?>? = builder.additionalVersionWeights

    // TODO - not real implementation, for debug only right now
    override fun toString(): String = buildString {
        appendln("AliasRoutingConfiguration(")
        appendln("\tadditionalVersionWeights: $additionalVersionWeights")
        appendln(")")
    }

    companion object {
        operator fun invoke(block: DslBuilder.() -> Unit) = BuilderImpl().apply(block).build()
        fun dslBuilder(): DslBuilder = BuilderImpl()
    }

    interface Builder {
        fun build(): AliasRoutingConfiguration
        // TODO - Java fill in Java builder
    }

    interface DslBuilder {
        // Location: Payload; Name: "AdditionalVersionWeights"
        var additionalVersionWeights: MutableMap<String, Float?>?

        fun build(): AliasRoutingConfiguration
    }

    private class BuilderImpl : Builder, DslBuilder {
        override var additionalVersionWeights: MutableMap<String, Float?>? = null

        override fun build(): AliasRoutingConfiguration = AliasRoutingConfiguration(this)
    }
}




/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
package aws.sdk.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.GradleConfiguration
import software.amazon.smithy.kotlin.codegen.KotlinDependency

// root namespace for the AWS client-runtime
const val AWS_CLIENT_RT_ROOT_NS = "aws.sdk.kotlin.runtime"
const val AWS_CLIENT_RT_AUTH_NS = "aws.sdk.kotlin.runtime.auth"
const val AWS_CLIENT_RT_REGIONS_NS = "aws.sdk.kotlin.runtime.regions"

// publishing info
const val AWS_CLIENT_RT_GROUP: String = "aws.sdk.kotlin"
// note: this version doesn't really matter since we substitute it for project dependency notation
// when generating client build files
const val AWS_CLIENT_RT_VERSION: String = "0.1.0-SNAPSHOT"

/**
 * Container object for AWS specific dependencies
 */
object AwsKotlinDependency {
    val AWS_CLIENT_RT_CORE = KotlinDependency(GradleConfiguration.Api, AWS_CLIENT_RT_ROOT_NS, AWS_CLIENT_RT_GROUP, "aws-client-rt", AWS_CLIENT_RT_VERSION)
    val AWS_CLIENT_RT_HTTP = KotlinDependency(GradleConfiguration.Api, "$AWS_CLIENT_RT_ROOT_NS.http", AWS_CLIENT_RT_GROUP, "http", AWS_CLIENT_RT_VERSION)
    val AWS_CLIENT_RT_AUTH = KotlinDependency(GradleConfiguration.Api, AWS_CLIENT_RT_AUTH_NS, AWS_CLIENT_RT_GROUP, "auth", AWS_CLIENT_RT_VERSION)
    val AWS_CLIENT_RT_REGIONS = KotlinDependency(GradleConfiguration.Api, AWS_CLIENT_RT_REGIONS_NS, AWS_CLIENT_RT_GROUP, "regions", AWS_CLIENT_RT_VERSION)
    val AWS_CLIENT_RT_JSON_PROTOCOLS = KotlinDependency(GradleConfiguration.Implementation, "$AWS_CLIENT_RT_ROOT_NS.protocol.json", AWS_CLIENT_RT_GROUP, "aws-json-protocols", AWS_CLIENT_RT_VERSION)
}

// remap aws-sdk-kotlin dependencies to project notation
private val sameProjectDeps = mapOf(
    AwsKotlinDependency.AWS_CLIENT_RT_CORE to """project(":client-runtime:aws-client-rt")""",
    AwsKotlinDependency.AWS_CLIENT_RT_HTTP to """project(":client-runtime:protocols:http")""",
    AwsKotlinDependency.AWS_CLIENT_RT_AUTH to """project(":client-runtime:auth")""",
    AwsKotlinDependency.AWS_CLIENT_RT_REGIONS to """project(":client-runtime:regions")""",
    AwsKotlinDependency.AWS_CLIENT_RT_JSON_PROTOCOLS to """project(":client-runtime:protocols:aws-json-protocols")"""
)

internal fun KotlinDependency.dependencyNotation(allowProjectNotation: Boolean = true): String {
    val dep = this
    return if (allowProjectNotation && sameProjectDeps.contains(dep)) {
        val projectNotation = sameProjectDeps[dep]
        "${dep.config}($projectNotation)"
    } else {
        "${dep.config}(\"${dep.group}:${dep.artifact}:${dep.version}\")"
    }
}

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
package software.amazon.smithy.aws.kotlin.codegen

import software.amazon.smithy.kotlin.codegen.GradleConfiguration
import software.amazon.smithy.kotlin.codegen.KotlinDependency

// root namespace for the AWS client-runtime
const val AWS_CLIENT_RT_ROOT_NS = "software.aws.kotlinsdk"

// publishing info
const val AWS_CLIENT_RT_GROUP = "software.aws.kotlin"
const val AWS_CLIENT_RT_VERSION = "0.0.1"

/**
 * Container object for AWS specific dependencies
 */
object AwsKotlinDependency {
    val REST_JSON_FEAT = KotlinDependency(GradleConfiguration.Implementation, "$AWS_CLIENT_RT_ROOT_NS.restjson", AWS_CLIENT_RT_GROUP, "rest-json", AWS_CLIENT_RT_VERSION)
}

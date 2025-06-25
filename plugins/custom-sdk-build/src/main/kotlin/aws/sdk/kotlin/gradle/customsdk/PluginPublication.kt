/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.customsdk

/**
 * Plugin publication metadata and configuration.
 * Contains information needed for publishing the plugin to Gradle Plugin Portal and Maven Central.
 */
object PluginPublication {
    
    const val PLUGIN_ID = "aws.sdk.kotlin.custom-sdk-build"
    const val PLUGIN_NAME = "AWS SDK for Kotlin Custom SDK Build Plugin"
    const val PLUGIN_DESCRIPTION = "Gradle plugin for generating custom AWS SDK clients with only selected operations"
    const val PLUGIN_URL = "https://github.com/awslabs/aws-sdk-kotlin"
    
    const val GROUP_ID = "aws.sdk.kotlin"
    const val ARTIFACT_ID = "custom-sdk-build-gradle-plugin"
    
    const val LICENSE_NAME = "Apache License 2.0"
    const val LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0"
    
    const val DEVELOPER_ID = "aws-sdk-kotlin-team"
    const val DEVELOPER_NAME = "AWS SDK for Kotlin Team"
    const val DEVELOPER_EMAIL = "aws-sdk-kotlin@amazon.com"
    
    const val SCM_CONNECTION = "scm:git:git://github.com/awslabs/aws-sdk-kotlin.git"
    const val SCM_DEVELOPER_CONNECTION = "scm:git:ssh://github.com/awslabs/aws-sdk-kotlin.git"
    const val SCM_URL = "https://github.com/awslabs/aws-sdk-kotlin"
    
    /**
     * Plugin tags for Gradle Plugin Portal.
     */
    val PLUGIN_TAGS = listOf(
        "aws",
        "sdk",
        "kotlin",
        "custom",
        "build",
        "codegen",
        "smithy"
    )
    
    /**
     * Plugin display name for Gradle Plugin Portal.
     */
    const val PLUGIN_DISPLAY_NAME = "AWS SDK for Kotlin Custom SDK Build"
    
    /**
     * Minimum Gradle version required.
     */
    const val MIN_GRADLE_VERSION = "7.0"
    
    /**
     * Minimum Java version required.
     */
    const val MIN_JAVA_VERSION = "11"
    
    /**
     * Compatible Kotlin versions.
     */
    val COMPATIBLE_KOTLIN_VERSIONS = listOf(
        "1.8.0",
        "1.9.0",
        "1.9.10",
        "1.9.20"
    )
}

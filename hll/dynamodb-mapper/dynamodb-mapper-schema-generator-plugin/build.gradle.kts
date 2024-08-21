/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
description = "Plugin used to generate DynamoDbMapper schemas from user classes"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Schema Generator Plugin"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.plugins"

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.plugin.publish)
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("dynamodb-mapper-schema-generator") {
            id = "aws.sdk.kotlin.hll.dynamodbmapper.schema.generator"
            implementationClass = "aws.sdk.kotlin.hll.dynamodbmapper.plugins.SchemaGeneratorPlugin"
            description = "Plugin used to generate DynamoDbMapper schemas from user classes"
        }
    }
}

val sdkVersion: String by project
group = "aws.sdk.kotlin"
version = sdkVersion


publishing {
    publications {
        create<MavenPublication>("dynamodb-mapper-schema-generator-plugin") {
            from(components["java"])
        }
    }
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(libs.ksp.gradle.plugin)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test)
}


tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showStackTraces = true
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

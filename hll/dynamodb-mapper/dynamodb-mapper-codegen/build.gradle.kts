/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "DynamoDbMapper code generation"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Codegen"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.codegen"

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

dependencies {
    implementation(libs.ksp.api)
    implementation(project(":hll:hll-codegen"))
    implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
    implementation(project(":services:dynamodb"))

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test.junit5)
}

kotlin {
    explicitApi()
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

val sdkVersion: String by project
group = "aws.sdk.kotlin"
version = sdkVersion

val sourcesJar by tasks.creating(Jar::class) {
    group = "publishing"
    description = "Assembles Kotlin sources jar"
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("dynamodb-mapper-codegen") {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}
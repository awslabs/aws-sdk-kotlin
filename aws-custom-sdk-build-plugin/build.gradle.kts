/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

group = "aws.sdk.kotlin"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Gradle API - use explicit Kotlin version matching parent project
    implementation(kotlin("gradle-plugin", version = "2.1.0"))
    
    // Kotlin reflection for constants registry
    implementation(kotlin("reflect", version = "2.1.0"))
    
    // Smithy dependencies for model processing - use versions matching parent project
    implementation("software.amazon.smithy:smithy-model:1.60.2")
    implementation("software.amazon.smithy:smithy-aws-traits:1.60.2")
    implementation("software.amazon.smithy:smithy-protocol-traits:1.60.2")
    
    // Smithy Kotlin codegen dependencies - use versions matching parent project
    implementation("software.amazon.smithy.kotlin:smithy-kotlin-codegen:0.34.21")
    
    // Testing dependencies - use versions matching parent project
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.5")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.0")
    testImplementation("io.mockk:mockk:1.13.13")
}

gradlePlugin {
    plugins {
        create("awsCustomSdkBuild") {
            id = "aws.sdk.kotlin.custom-sdk-build"
            implementationClass = "aws.sdk.kotlin.gradle.customsdk.AwsCustomSdkBuildPlugin"
            displayName = "AWS Custom SDK Build Plugin"
            description = "Generate lightweight AWS service clients with only selected operations"
        }
    }
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

// Configure publishing for local development
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

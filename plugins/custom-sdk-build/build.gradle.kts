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

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("gradle-plugin"))
    compileOnly(kotlin("gradle-plugin-api"))

    // Smithy dependencies for codegen integration
    implementation(libs.smithy.model)
    implementation(libs.smithy.aws.traits)
    implementation(libs.smithy.protocol.traits)
    implementation(libs.smithy.kotlin.codegen)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(gradleTestKit())
}

gradlePlugin {
    website.set("https://github.com/awslabs/aws-sdk-kotlin")
    vcsUrl.set("https://github.com/awslabs/aws-sdk-kotlin.git")
    
    plugins {
        create("custom-sdk-build") {
            id = "aws.sdk.kotlin.custom-sdk-build"
            implementationClass = "aws.sdk.kotlin.gradle.customsdk.CustomSdkBuildPlugin"
            displayName = "AWS SDK for Kotlin Custom SDK Build"
            description = "Gradle plugin for generating custom AWS SDK clients with only selected operations"
            tags.set(listOf("aws", "sdk", "kotlin", "custom", "build", "codegen", "smithy"))
        }
    }
}

// Configure plugin publication
publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            groupId = "aws.sdk.kotlin"
            artifactId = "custom-sdk-build-gradle-plugin"
            
            pom {
                name.set("AWS SDK for Kotlin Custom SDK Build Plugin")
                description.set("Gradle plugin for generating custom AWS SDK clients with only selected operations")
                url.set("https://github.com/awslabs/aws-sdk-kotlin")
                
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                
                developers {
                    developer {
                        id.set("aws-sdk-kotlin-team")
                        name.set("AWS SDK for Kotlin Team")
                        email.set("aws-sdk-kotlin@amazon.com")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/awslabs/aws-sdk-kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/awslabs/aws-sdk-kotlin.git")
                    url.set("https://github.com/awslabs/aws-sdk-kotlin")
                }
            }
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

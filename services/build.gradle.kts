/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import java.time.LocalDateTime
import aws.sdk.kotlin.gradle.kmp.*

buildscript {
    dependencies {
        // Add our custom gradle plugin(s) to buildscript classpath (comes from github source)
        classpath("aws.sdk.kotlin:build-plugins") {
            version {
                branch = "kmp-plugin"
            }
        }
    }
}

plugins {
    id("org.jetbrains.dokka")
}

apply(plugin="aws.sdk.kotlin.kmp")

val sdkVersion: String by project
val smithyKotlinVersion: String by project
val kotlinVersion: String by project
val coroutinesVersion: String by project
val kotestVersion: String by project
val slf4jVersion: String by project

val optinAnnotations = listOf(
    "aws.smithy.kotlin.runtime.InternalApi",
    "aws.sdk.kotlin.runtime.InternalSdkApi",
    "kotlin.RequiresOptIn",
)

subprojects {
    group = "aws.sdk.kotlin"
    version = sdkVersion

    apply {
        plugin("org.jetbrains.kotlin.multiplatform")
        plugin("org.jetbrains.dokka")
    }

    logger.info("configuring: $project")

    kotlin {
        explicitApi()

        sourceSets {
            all {
                // have generated sdk's opt-in to internal runtime features
                optinAnnotations.forEach { languageSettings.optIn(it) }
            }

            getByName("commonMain") {
                kotlin.srcDir("generated-src/main/kotlin")
            }

            getByName("commonTest") {
                kotlin.srcDir("generated-src/test")

                dependencies {
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
                }
            }
        }

        if (project.file("e2eTest").exists()) {
            jvm().compilations {
                val e2eTest by creating {
                    defaultSourceSet {
                        kotlin.srcDir("e2eTest/src")
                        resources.srcDir("e2eTest/test-resources")
                        dependsOn(sourceSets.getByName("commonMain"))
                        dependsOn(sourceSets.getByName("jvmMain"))

                        dependencies {
                            api("aws.smithy.kotlin:testing:$smithyKotlinVersion")
                            implementation(kotlin("test"))
                            implementation(kotlin("test-junit5"))
                            implementation(project(":tests:e2e-test-util"))
                            implementation("org.slf4j:slf4j-simple:$slf4jVersion")
                        }
                    }

                    kotlinOptions {
                        // Enable coroutine runTests in 1.6.10
                        // NOTE: may be removed after coroutines-test runTests becomes stable
                        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
                    }

                    tasks.register<Test>("e2eTest") {
                        description = "Run e2e service tests"
                        group = "verification"

                        // Run the tests with the classpath containing the compile dependencies (including 'main'),
                        // runtime dependencies, and the outputs of this compilation:
                        classpath = compileDependencyFiles + runtimeDependencyFiles + output.allOutputs

                        // Run only the tests from this compilation's outputs:
                        testClassesDirs = output.classesDirs

                        useJUnitPlatform()
                        testLogging {
                            events("passed", "skipped", "failed")
                            showStandardStreams = true
                            showStackTraces = true
                            showExceptions = true
                            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                        }

                        // model a random input to enable re-running e2e tests back to back without
                        // up-to-date checks or cache getting in the way
                        inputs.property("integration.datetime", LocalDateTime.now())
                        systemProperty("org.slf4j.simpleLogger.defaultLogLevel", System.getProperty("org.slf4j.simpleLogger.defaultLogLevel", "WARN"))
                    }
                }
            }
        }
    }

    dependencies {
        dokkaPlugin(project(":dokka-aws"))
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            allWarningsAsErrors = false // FIXME Tons of errors occur in generated code
            jvmTarget = "1.8" // fixes outgoing variant metadata: https://github.com/awslabs/smithy-kotlin/issues/258
        }
    }

    apply(from = rootProject.file("gradle/publish.gradle"))
}

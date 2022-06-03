/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.dokka")
}

val platforms = listOf("common", "jvm")

val sdkVersion: String by project
val smithyKotlinVersion: String by project
val kotlinVersion: String by project
val coroutinesVersion: String by project
val kotestVersion: String by project

val optinAnnotations = listOf(
    "aws.smithy.kotlin.runtime.util.InternalApi",
    "aws.sdk.kotlin.runtime.InternalSdkApi",
    "kotlin.RequiresOptIn",
)

kotlin {
    jvm() // Create a JVM target with the default name 'jvm'
}

subprojects {
    group = "aws.sdk.kotlin"
    version = sdkVersion

    apply {
        plugin("org.jetbrains.kotlin.multiplatform")
        plugin("org.jetbrains.dokka")
    }

    logger.info("configuring: $project")

    platforms.forEach { platform ->
        configure(listOf(project)) {
            apply(from = rootProject.file("gradle/$platform.gradle"))
        }
    }

    kotlin {
        sourceSets {
            all {
                val srcDir = if (name.endsWith("Main")) "src" else "test"
                val resourcesPrefix = if (name.endsWith("Test")) "test-" else  ""
                // the name is always the platform followed by a suffix of either "Main" or "Test" (e.g. jvmMain, commonTest, etc)
                val platform = name.substring(0, name.length - 4)
                kotlin.srcDir("$platform/$srcDir")
                resources.srcDir("$platform/${resourcesPrefix}resources")

                languageSettings.progressiveMode = true

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
                    implementation(project(":aws-runtime:testing"))
                }
            }
        }

        if (project.file("e2eTest").exists()) {
            jvm().compilations {
                val main by getting
                val e2eTest by creating {
                    defaultSourceSet {
                        kotlin.srcDir("e2eTest")

                        dependencies {
                            // Compile against the main compilation's compile classpath and outputs:
                            implementation(main.compileDependencyFiles + main.runtimeDependencyFiles + main.output.classesDirs)

                            implementation(kotlin("test"))
                            implementation(kotlin("test-junit5"))
                            implementation(project(":aws-runtime:testing"))
                            implementation("aws.smithy.kotlin:hashing:$smithyKotlinVersion")
                            implementation(project(":tests:e2e-test-util"))
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

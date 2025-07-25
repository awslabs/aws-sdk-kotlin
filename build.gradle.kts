/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configureJReleaser
import aws.sdk.kotlin.gradle.dsl.configureLinting
import aws.sdk.kotlin.gradle.util.typedProp

buildscript {
    // NOTE: buildscript classpath for the root project is the parent classloader for the subprojects, we
    // only need to add e.g. atomic-fu and build-plugins here for imports and plugins to be available in subprojects.
    dependencies {
        classpath(libs.kotlinx.atomicfu.plugin)
        // Add our custom gradle build logic to buildscript classpath
        classpath(libs.aws.kotlin.repo.tools.build.support)
        /*
        Enforce jackson to a version supported both by dokka and jreleaser:
        https://github.com/Kotlin/dokka/issues/3472#issuecomment-1929712374
        https://github.com/Kotlin/dokka/issues/3194#issuecomment-1929382630
         */
        classpath(enforcedPlatform("com.fasterxml.jackson:jackson-bom:2.15.3"))
    }
}

plugins {
    `dokka-convention`
    // ensure the correct version of KGP ends up on our buildscript classpath
    id(libs.plugins.kotlin.multiplatform.get().pluginId) apply false
    id(libs.plugins.kotlin.jvm.get().pluginId) apply false
    alias(libs.plugins.aws.kotlin.repo.tools.artifactsizemetrics)
}

artifactSizeMetrics {
    artifactPrefixes = setOf(":services", ":aws-runtime")
    closurePrefixes = setOf(":services")
    significantChangeThresholdPercentage = 5.0
    projectRepositoryName = "aws-sdk-kotlin"
}

val testJavaVersion = typedProp<String>("test.java.version")?.let {
    JavaLanguageVersion.of(it)
}?.also {
    println("configuring tests to run with jdk $it")
}

allprojects {
    if (rootProject.typedProp<Boolean>("kotlinWarningsAsErrors") == true) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            compilerOptions.allWarningsAsErrors = true
        }
    }

    if (testJavaVersion != null) {
        tasks.withType<Test> {
            val toolchains = project.extensions.getByType<JavaToolchainService>()
            javaLauncher.set(
                toolchains.launcherFor {
                    languageVersion.set(testJavaVersion)
                },
            )
        }
    }

    // Enables running `./gradlew allDeps` to get a comprehensive list of dependencies for every subproject
    tasks.register<DependencyReportTask>("allDeps") { }
}

// Configure root module's documentation
dokka {
    moduleName.set("AWS SDK for Kotlin")

    dokkaPublications.html {
        includes.from(
            rootProject.file("docs/dokka-presets/README.md"),
        )
    }
}

// Aggregate subprojects' documentation
dependencies {
    dokka(project(":aws-runtime"))
    dokka(project(":services"))
    dokka(project(":hll"))
}

// Publishing
configureJReleaser()

// Code Style
val lintPaths = listOf(
    "**/*.{kt,kts}",
    "!**/generated-src/**",
    "!**/generated/ksp/**",
    "!**/kspCaches/**",
    "!**/smithyprojections/**",
    "!**/build/**",
)

configureLinting(lintPaths)

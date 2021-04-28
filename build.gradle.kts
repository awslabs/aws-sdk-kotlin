/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.4.32")
    }
}

plugins {
    kotlin("jvm") version "1.4.31" apply false
    id("org.jetbrains.dokka")
}


allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        // for dokka
        maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") {
            content {
                includeGroup("org.jetbrains.kotlinx")
            }
        }
    }

    tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
        outputDirectory.set(buildDir.resolve("dokka"))
    }
    tasks.withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask>().configureEach {
        outputDirectory.set(buildDir.resolve("dokkaMultiModule"))
    }
    tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaTask>().configureEach {
        val sdkVersion: String by project
        moduleVersion.set(sdkVersion)

        val year = java.time.LocalDate.now().year
        pluginsMapConfiguration.put("org.jetbrains.dokka.base.DokkaBase", """
            {
                "customStyleSheets": ["${rootProject.file("docs/api/css/aws.css")}"],
                "customAssets": [
                    "${rootProject.file("docs/api/assets/logo-icon.svg")}"
                ],
                "footerMessage": "© $year, Amazon Web Services, Inc. or its affiliates. All rights reserved."
            }
        """)

    }
}

// configure the root multimodule docs
tasks.dokkaHtmlMultiModule {
    includes.from(rootProject.file("docs/GettingStarted.md"))
    moduleName.set("AWS Kotlin SDK")
}

val ktlint: Configuration by configurations.creating
val ktlintVersion: String by project
dependencies {
    ktlint("com.pinterest:ktlint:$ktlintVersion")
}

val lintPaths = listOf(
    "codegen/smithy-aws-kotlin-codegen/**/*.kt",
    "client-runtime/**/*.kt",
    "examples/**/*.kt"
)

tasks.register<JavaExec>("ktlint") {
    description = "Check Kotlin code style."
    group = "Verification"
    classpath = configurations.getByName("ktlint")
    main = "com.pinterest.ktlint.Main"
    args = lintPaths
}

tasks.register<JavaExec>("ktlintFormat") {
    description = "Auto fix Kotlin code style violations"
    group = "formatting"
    classpath = configurations.getByName("ktlint")
    main = "com.pinterest.ktlint.Main"
    args = listOf("-F") + lintPaths
}

// configure coverage for the entire project
apply(from = rootProject.file("gradle/codecoverage.gradle"))

tasks.register("showRepos") {
    doLast {
        println("All repos:")
        println(repositories.map { it.name })
    }
}

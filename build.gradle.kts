/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
plugins {
    kotlin("jvm") version "1.5.0" apply false
    id("org.jetbrains.dokka")
}

dependencies {
    dokkaPlugin(project(":dokka-aws"))
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

    tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
        // each module can include their own top-level module documentation
        // see https://kotlinlang.org/docs/kotlin-doc.html#module-and-package-documentation
        if (project.file("API.md").exists()) {
            dokkaSourceSets.configureEach {
                includes.from(project.file("API.md"))
            }
        }
    }

    tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaTask>().configureEach {
        val sdkVersion: String by project
        moduleVersion.set(sdkVersion)

        val year = java.time.LocalDate.now().year
        val pluginConfigMap = mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """
                {
                    "customStyleSheets": ["${rootProject.file("docs/api/css/logo-styles.css")}"],
                    "customAssets": [
                        "${rootProject.file("docs/api/assets/logo-icon.svg")}",
                        "${rootProject.file("docs/api/assets/aws_logo_white_59x35.png")}"
                    ],
                    "footerMessage": "© $year, Amazon Web Services, Inc. or its affiliates. All rights reserved."
                }
            """
        )
        pluginsMapConfiguration.set(pluginConfigMap)
    }
}

// configure the root multimodule docs
tasks.dokkaHtmlMultiModule {
    moduleName.set("AWS Kotlin SDK")

    includes.from(
        // NOTE: these get concatenated
        rootProject.file("docs/api/README.md"),
        rootProject.file("docs/GettingStarted.md")
    )

    val excludeFromDocumentation = listOf(
        project(":client-runtime:testing"),
        project(":client-runtime:crt-util")
    )
    removeChildTasks(excludeFromDocumentation)
}

val ktlint: Configuration by configurations.creating
val ktlintVersion: String by project
dependencies {
    ktlint("com.pinterest:ktlint:$ktlintVersion")
}

val lintPaths = listOf(
    "codegen/smithy-aws-kotlin-codegen/**/*.kt",
    "client-runtime/**/*.kt",
    "examples/**/*.kt",
    "dokka-aws/**/*.kt",
    "build-tools/**/*.kt",
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

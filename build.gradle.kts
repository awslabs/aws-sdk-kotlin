/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configureLinting
import aws.sdk.kotlin.gradle.dsl.configureNexus
import aws.sdk.kotlin.gradle.kmp.configureIosSimulatorTasks
import aws.sdk.kotlin.gradle.util.typedProp
import java.net.URL

buildscript {
    // NOTE: buildscript classpath for the root project is the parent classloader for the subprojects, we
    // only need to add e.g. atomic-fu and build-plugins here for imports and plugins to be available in subprojects.
    dependencies {
        classpath(libs.kotlinx.atomicfu.plugin)
        // Add our custom gradle build logic to buildscript classpath
        classpath(libs.aws.kotlin.repo.tools.build.support)
    }
}

plugins {
    alias(libs.plugins.dokka)
    // ensure the correct version of KGP ends up on our buildscript classpath
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.aws.kotlin.repo.tools.artifactsizemetrics)
    alias(libs.plugins.aws.kotlin.repo.tools.kmp)
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
    tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaTask>().configureEach {
        val sdkVersion: String by project
        moduleVersion.set(sdkVersion)

        val year = java.time.LocalDate.now().year
        val pluginConfigMap = mapOf(
            "org.jetbrains.dokka.base.DokkaBase" to """
                {
                    "customStyleSheets": [
                        "${rootProject.file("docs/dokka-presets/css/logo-styles.css")}",
                        "${rootProject.file("docs/dokka-presets/css/aws-styles.css")}"
                    ],
                    "customAssets": [
                        "${rootProject.file("docs/dokka-presets/assets/logo-icon.svg")}",
                        "${rootProject.file("docs/dokka-presets/assets/aws_logo_white_59x35.png")}",
                        "${rootProject.file("docs/dokka-presets/scripts/accessibility.js")}"
                    ],
                    "footerMessage": "© $year, Amazon Web Services, Inc. or its affiliates. All rights reserved.",
                    "separateInheritedMembers" : true,
                    "templatesDir": "${rootProject.file("docs/dokka-presets/templates")}"
                }
            """,
        )
        pluginsMapConfiguration.set(pluginConfigMap)
    }

    tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
        // each module can include their own top-level module documentation
        // see https://kotlinlang.org/docs/dokka-module-and-package-docs.html
        if (project.file("API.md").exists()) {
            dokkaSourceSets.configureEach {
                includes.from(project.file("API.md"))
            }
        }

        dokkaSourceSets.configureEach {
            samples.from(project.file("samples").path, project.file("generated-src/samples").path)
        }

        val smithyKotlinPackageListUrl: String? by project
        val smithyKotlinDocBaseUrl: String? by project

        // Configure Dokka to link to smithy-kotlin types if specified in properties
        // These optional properties are supplied api the api docs build job but are unneeded otherwise
        smithyKotlinDocBaseUrl.takeUnless { it.isNullOrEmpty() }?.let { docBaseUrl ->
            val expandedDocBaseUrl = docBaseUrl.replace("\$smithyKotlinRuntimeVersion", libs.versions.smithy.kotlin.runtime.version.get())
            dokkaSourceSets.configureEach {
                externalDocumentationLink {
                    url.set(URL(expandedDocBaseUrl))

                    smithyKotlinPackageListUrl
                        .takeUnless { it.isNullOrEmpty() }
                        ?.let { packageListUrl.set(URL(it)) }
                }
            }
        }
    }

    if (rootProject.typedProp<Boolean>("kotlinWarningsAsErrors") == true) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.allWarningsAsErrors = true
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

    configureIosSimulatorTasks()
}

project.afterEvaluate {
    // configure the root multimodule docs
    tasks.dokkaHtmlMultiModule.configure {
        moduleName.set("AWS SDK for Kotlin")

        // Output subprojects' docs to <docs-base>/project-name/* instead of <docs-base>/path/to/project-name/*
        // This is especially important for inter-repo linking (e.g., via externalDocumentationLink) because the
        // package-list doesn't contain enough project path information to indicate where modules' documentation are
        // located.
        fileLayout.set { parent, child ->
            parent.outputDirectory.dir(child.moduleName)
        }

        includes.from(
            // NOTE: these get concatenated
            rootProject.file("docs/dokka-presets/README.md"),
        )
    }
}

// Publishing
configureNexus()

// Code Style
val lintPaths = listOf(
    "**/*.{kt,kts}",
    "!**/generated-src/**",
    "!**/generated/ksp/**",
    "!**/kspCaches/**",
    "!**/smithyprojections/**",
)

configureLinting(lintPaths)

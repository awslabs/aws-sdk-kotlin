/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.util.typedProp
import aws.sdk.kotlin.gradle.dsl.configureLinting
import java.net.URL
import java.time.Duration

buildscript {
    dependencies {
        // Add our custom gradle plugin(s) to buildscript classpath (comes from github source)
        // NOTE: buildscript classpath for the root project is the parent classloader for the subprojects, we
        // only need to include it here, imports in subprojects will work automagically
        classpath("aws.sdk.kotlin:build-plugins") {
            version {
                // require("0.1.1")
                branch = "lint-rules"
            }
        }
    }
}

plugins {
    kotlin("jvm") version "1.8.22" apply false
    id("org.jetbrains.dokka")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

// configures (KMP) subprojects with our own KMP conventions and some default dependencies
apply(plugin = "aws.sdk.kotlin.kmp")

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }

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
                        "${rootProject.file("docs/dokka-presets/assets/aws_logo_white_59x35.png")}"
                    ],
                    "footerMessage": "© $year, Amazon Web Services, Inc. or its affiliates. All rights reserved.",
                    "separateInheritedMembers" : true,
                    "templatesDir": "${rootProject.file("docs/dokka-presets/templates")}"
                }
            """,
        )
        pluginsMapConfiguration.set(pluginConfigMap)
    }
}

subprojects {
    tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
        // each module can include their own top-level module documentation
        // see https://kotlinlang.org/docs/kotlin-doc.html#module-and-package-documentation
        if (project.file("API.md").exists()) {
            dokkaSourceSets.configureEach {
                includes.from(project.file("API.md"))
            }
        }

        val smithyKotlinPackageListUrl: String? by project
        val smithyKotlinDocBaseUrl: String? by project
        val smithyKotlinVersion: String by project

        // Configure Dokka to link to smithy-kotlin types if specified in properties
        // These optional properties are supplied api the api docs build job but are unneeded otherwise
        smithyKotlinDocBaseUrl.takeUnless { it.isNullOrEmpty() }?.let { docBaseUrl ->
            val expandedDocBaseUrl = docBaseUrl.replace("\$smithyKotlinVersion", smithyKotlinVersion)
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
}

if (project.typedProp<Boolean>("kotlinWarningsAsErrors") == true) {
    subprojects {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.allWarningsAsErrors = true
        }
    }
}

project.afterEvaluate {
    // configure the root multimodule docs
    tasks.dokkaHtmlMultiModule.configure {
        moduleName.set("AWS SDK for Kotlin")

        // Output subprojects' docs to <docs-base>/project-name/* instead of <docs-base>/path/to/project-name/*
        // This is especially important for inter-repo linking (e.g., via externalDocumentationLink) because the
        // package-list doesn't contain enough project path information to indicate where modules' documentation are
        // located.
        fileLayout.set { parent, child -> parent.outputDirectory.get().resolve(child.project.name) }

        includes.from(
            // NOTE: these get concatenated
            rootProject.file("docs/dokka-presets/README.md"),
        )
    }
}

if (
    project.hasProperty("sonatypeUsername") &&
    project.hasProperty("sonatypePassword") &&
    project.hasProperty("publishGroupName")
) {
    apply(plugin = "io.github.gradle-nexus.publish-plugin")

    val publishGroupName = project.property("publishGroupName") as String
    group = publishGroupName

    nexusPublishing {
        repositories {
            create("awsNexus") {
                nexusUrl.set(uri("https://aws.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://aws.oss.sonatype.org/content/repositories/snapshots/"))
                username.set(project.property("sonatypeUsername") as String)
                password.set(project.property("sonatypePassword") as String)
            }
        }

        transitionCheckOptions {
            maxRetries.set(180)
            delayBetween.set(Duration.ofSeconds(10))
        }
    }
}

val lintPaths = listOf(
    "**/*.{kt,kts}",
    "!**/generated-src/**",
    "!**/smithyprojections/**",
)

configureLinting(lintPaths)

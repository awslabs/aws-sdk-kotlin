/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configureLinting
import aws.sdk.kotlin.gradle.dsl.configureNexus
import aws.sdk.kotlin.gradle.util.typedProp
import org.jsoup.Jsoup
import java.net.URL

buildscript {
    // NOTE: buildscript classpath for the root project is the parent classloader for the subprojects, we
    // only need to add e.g. atomic-fu and build-plugins here for imports and plugins to be available in subprojects.
    dependencies {
        classpath(libs.kotlinx.atomicfu.plugin)
        // Add our custom gradle build logic to buildscript classpath
        classpath(libs.aws.kotlin.repo.tools.build.support)
        classpath(libs.jsoup)
    }
}

plugins {
    alias(libs.plugins.dokka)
    // ensure the correct version of KGP ends up on our buildscript classpath
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
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
                        "${rootProject.file("docs/dokka-presets/scripts/accessibility.js")}",
                        "${rootProject.file("docs/dokka-presets/scripts/custom-navigation-loader.js")}"
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
}

project.afterEvaluate {
    val trimNavigations = tasks.register("trimNavigations") {
        description = "Trims navigation files to remove unrelated child submenu"
        group = "documentation"

        doLast {
            val dokkaOutputDir = rootProject.buildDir.resolve("dokka/htmlMultiModule")

            if (!dokkaOutputDir.exists()) {
                logger.warn("Dokka output directory not found: ${dokkaOutputDir.absolutePath}")
                logger.warn("Skipping navigation trimming")
                return@doLast
            }

            dokkaOutputDir.listFiles { file ->
                file.isDirectory && file.resolve("navigation.html").exists()
            }?.forEach { moduleDir ->
                val moduleName = moduleDir.name

                val navFile = File(moduleDir, "navigation.html")

                if (navFile.exists()) {
                    val doc = Jsoup.parse(navFile, "UTF-8")

                    // Fix navigation links
                    doc.select("a[href^='../']").forEach { anchor ->
                        val originalHref = anchor.attr("href")
                        val trimmedHref = originalHref.replace("../", "")
                        anchor.attr("href", trimmedHref)
                    }

                    val sideMenuParts = doc.select("div.sideMenu > div.sideMenuPart")

                    sideMenuParts.forEach { submenu ->
                        val submenuId = submenu.id()
                        // If this is not the current module's submenu, remove all its nested content
                        if (submenuId != "$moduleName-nav-submenu") {
                            val overviewDiv = submenu.select("> div.overview").first()
                            overviewDiv?.select("span.navButton")?.remove()
                            submenu.children().remove()
                            if (overviewDiv != null) {
                                submenu.appendChild(overviewDiv)
                            }
                        }
                    }

                    val wrappedContent = "<div class=\"sideMenu\">\n${sideMenuParts.outerHtml()}\n</div>"
                    navFile.writeText(wrappedContent)
                }
            }
        }
    }

    val useCustomNavigations = tasks.register("useCustomNavigations") {
        group = "documentation"
        description = "Replace default Dokka navigation-loader.js with custom implementation"

        doLast {
            val dokkaOutputDir = rootProject.buildDir.resolve("dokka/htmlMultiModule")

            if (!dokkaOutputDir.exists()) {
                logger.warn("Dokka output directory not found: ${dokkaOutputDir.absolutePath}")
                logger.warn("Skipping using custom navigations")
                return@doLast
            }

            dokkaOutputDir.walkTopDown()
                .filter { it.isFile && it.name.endsWith(".html") }
                .forEach { file ->
                    val updatedContent = file.readLines().filterNot { line ->
                        line.contains("""scripts/navigation-loader.js""")
                    }.joinToString("\n")

                    file.writeText(updatedContent)
                }
        }
    }

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

        finalizedBy(trimNavigations, useCustomNavigations)
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

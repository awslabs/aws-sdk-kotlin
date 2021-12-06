/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import java.util.Properties
import java.net.URL

plugins {
    kotlin("jvm") version "1.5.31" apply false
    id("org.jetbrains.dokka")
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
}

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
                    "customStyleSheets": ["${rootProject.file("docs/dokka-presets/css/logo-styles.css")}"],
                    "customAssets": [
                        "${rootProject.file("docs/dokka-presets/assets/logo-icon.svg")}",
                        "${rootProject.file("docs/dokka-presets/assets/aws_logo_white_59x35.png")}"
                    ],
                    "footerMessage": "© $year, Amazon Web Services, Inc. or its affiliates. All rights reserved.",
                    "separateInheritedMembers" : true
                }
            """
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

        val smithyKotlinPackageListUrl: String by project
        val smithyKotlinDocBaseUrl: String by project
        // Configure Dokka to link to smithy-kotlin types
        dokkaSourceSets.configureEach {
            externalDocumentationLink {
                packageListUrl.set(URL(smithyKotlinPackageListUrl))
                url.set(URL(smithyKotlinDocBaseUrl))
            }
        }
    }
}

val localProperties: Map<String, Any> by lazy {
    val props = Properties()

    listOf(
        File(rootProject.projectDir, "local.properties"), // Project-specific local properties
        File(rootProject.projectDir.parent, "local.properties"), // Workspace-specific local properties
        File(System.getProperty("user.home"), ".sdkdev/local.properties"), // User-specific local properties
    )
        .filter(File::exists)
        .map(File::inputStream)
        .forEach(props::load)

    props.mapKeys { (k, _) -> k.toString() }
}

fun Project.prop(name: String): Any? =
    this.properties[name] ?: localProperties[name]

if (project.prop("kotlinWarningsAsErrors")?.toString()?.toBoolean() == true) {
    subprojects {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.allWarningsAsErrors = true
        }
    }
}

// configure the root multimodule docs
tasks.dokkaHtmlMultiModule.configure {
    moduleName.set("AWS Kotlin SDK")

    includes.from(
        // NOTE: these get concatenated
        rootProject.file("docs/dokka-presets/README.md"),
    )

    val excludeFromDocumentation = listOf(
        project(":aws-runtime:testing"),
        project(":aws-runtime:crt-util"),
    )
    removeChildTasks(excludeFromDocumentation)

    // This allows docs generation to be overridden on the command line.
    // Used to generate each AWS service individually.
    if (project.hasProperty("dokkaOutSubDir")) {
        val subDir = project.prop("dokkaOutSubDir");
        val targetDir = buildDir.resolve("dokka/$subDir")
        println("Generating docs in $targetDir")
        outputDirectory.set(targetDir)
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
    }
}

val ktlint: Configuration by configurations.creating {
    attributes {
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
}
val ktlintVersion: String by project
dependencies {
    ktlint("com.pinterest:ktlint:$ktlintVersion")
}

val lintPaths = listOf(
    "codegen/smithy-aws-kotlin-codegen/**/*.kt",
    "aws-runtime/**/*.kt",
    "examples/**/*.kt",
    "dokka-aws/**/*.kt",
    "services/**/*.kt",
    "!services/*/generated-src/**/*.kt"
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

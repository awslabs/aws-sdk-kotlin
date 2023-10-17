/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configurePublishing
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import java.util.*

plugins {
    `maven-publish`
    `java-platform`
    `version-catalog`
}

val sdkVersion: String by project

group = "aws.sdk.kotlin"
version = sdkVersion

val evaluateAfter = listOf(":services", ":aws-runtime", ":tests", ":codegen")
evaluateAfter.forEach { evaluationDependsOn(it) }

fun createBomConstraintsAndVersionCatalog() {
    val bomConstraints: DependencyConstraintHandler = dependencies.constraints
    val catalogExt = catalog

    rootProject.subprojects {
        val subproject = this
        val hasMavenPublish = subproject.plugins.hasPlugin("maven-publish")
        if (!hasMavenPublish) {
            logger.info("skipping bom and version-catalog entry for ${subproject.name}")
            return@subprojects
        }
        subproject.plugins.withType<KotlinMultiplatformPluginWrapper> {
            subproject.extensions.getByType<KotlinMultiplatformExtension>().targets.all {
                val target = this
                val gavCoordinates = gav(target)
                bomConstraints.api(gavCoordinates)
                catalogExt.versionCatalog {
                    val prefix = when {
                        subproject.path.contains(":services") -> "services-"
                        subproject.path.contains(":aws-runtime") -> "runtime-"
                        else -> ""
                    }
                    val alias = prefix + artifactId(target)
                    library(alias, gavCoordinates)
                }
            }
        }
    }


    // add smithy-kotlin versions to our BOM and allow direct aliasing in the catalog
    val smithyKotlinCatalog = extensions.getByType<VersionCatalogsExtension>().named("smithyKotlin")
    catalogExt.versionCatalog {
        smithyKotlinCatalog.libraryAliases.forEach {  alias ->
            val coordinates = smithyKotlinCatalog.findLibrary(alias).get()
            bomConstraints.api(coordinates)
            val newAlias = "runtime-smithykotlin-$alias"
            library(newAlias, coordinates.get().toString())
        }
    }


}

fun Project.artifactId(target: KotlinTarget): String = when (target) {
    is KotlinMetadataTarget -> name
    is KotlinJsTarget -> "$name-js"
    else -> "$name-${target.targetName.toLowerCase(Locale.ROOT)}"
}

/**
 * Returns a string like "aws.sdk.kotlin:s3-linuxx64:1.0.2" for this target.
 */
fun Project.gav(target: KotlinTarget): String {
    val artifactId = artifactId(target)
    return "$group:$artifactId:$version"
}

fun DependencyConstraintHandler.api(constraintNotation: Any) =
    add("api", constraintNotation)

createBomConstraintsAndVersionCatalog()

configurePublishing("aws-sdk-kotlin")

publishing {
    publications {
        create("bom", MavenPublication::class) {
            artifactId = "bom"
            from(project.components.getByName("javaPlatform"))
        }

        create<MavenPublication>("versionCatalog") {
            artifactId = "version-catalog"
            from(components["versionCatalog"])
        }
    }
}

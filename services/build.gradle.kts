/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 *
 */
plugins {
    kotlin("jvm")
    `maven`
    `maven-publish`
}

val sdkVersion: String by project

val experimentalAnnotations = listOf(
    "software.aws.clientrt.util.InternalAPI",
    "aws.sdk.kotlin.runtime.InternalSdkApi"
)

subprojects {
    group = "aws.sdk.kotlin"
    version = sdkVersion

    apply {
        plugin("org.jetbrains.kotlin.jvm")
    }

    // have generated sdk's opt-in to internal runtime features
    kotlin.sourceSets.all {
        experimentalAnnotations.forEach { languageSettings.useExperimentalAnnotation(it) }
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }


    // FIXME - kotlin multiplatform configures publications for you so when we switch we can remove this
    // and just apply "publish.gradle" from the set of root gradle scripts (just like we do for the runtime)
    plugins.apply("maven")
    plugins.apply("maven-publish")
    publishing {
        publications {
            create<MavenPublication>("sdk"){
                println("components: $components")
                from(components["java"])
            }
        }
    }
    apply(from = rootProject.file("gradle/publish.gradle"))

}
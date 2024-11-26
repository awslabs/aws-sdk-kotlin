/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

description = "Common code-generation utilities used by AWS SDK for Kotlin's high level libraries"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: Codegen"
extra["moduleName"] = "aws.sdk.kotlin.hll.codegen"

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

val optinAnnotations = listOf(
    "aws.smithy.kotlin.runtime.InternalApi",
    "aws.sdk.kotlin.runtime.InternalSdkApi",
    "kotlin.RequiresOptIn",
)

kotlin {
    explicitApi()

    sourceSets.all {
        optinAnnotations.forEach(languageSettings::optIn)
    }
}

dependencies {
    api(project(":aws-runtime:aws-core"))
    implementation(libs.ksp.api)
    implementation(libs.smithy.kotlin.runtime.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test.junit5)
}

val sourcesJar by tasks.creating(Jar::class) {
    group = "publishing"
    description = "Assembles Kotlin sources jar"
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("hll-codegen") {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}

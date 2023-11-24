/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.dsl.configurePublishing
plugins {
    kotlin("jvm")
    jacoco
    `maven-publish`
}

val sdkVersion: String by project
description = "Codegen support for AWS protocols"
group = "software.amazon.smithy.kotlin"
version = sdkVersion

dependencies {

    implementation(libs.kotlin.stdlib.jdk8)
    api(libs.smithy.kotlin.codegen)

    api(libs.smithy.aws.traits)
    api(libs.smithy.aws.iam.traits)
    api(libs.smithy.aws.cloudformation.traits)
    api(libs.smithy.protocol.test.traits)
    implementation(libs.smithy.aws.endpoints)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.smithy.kotlin.codegen.testutils)

    testImplementation(libs.slf4j.api)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.kotlinx.serialization.json)
}

val generateSdkRuntimeVersion by tasks.registering {
    // generate the version of the runtime to use as a resource.
    // this keeps us from having to manually change version numbers in multiple places
    val resourcesDir = "$buildDir/resources/main/aws/sdk/kotlin/codegen"
    val versionFile = file("$resourcesDir/sdk-version.txt")
    val gradlePropertiesFile = rootProject.file("gradle.properties")
    inputs.file(gradlePropertiesFile)
    outputs.file(versionFile)
    sourceSets.main.get().output.dir(resourcesDir)
    doLast {
        versionFile.writeText("$version")
    }
}

val jvmTargetVersion = JavaVersion.VERSION_17.toString()

tasks.compileKotlin {
    kotlinOptions.jvmTarget = jvmTargetVersion
    dependsOn(generateSdkRuntimeVersion)
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = jvmTargetVersion
}

tasks.withType<JavaCompile> {
    sourceCompatibility = jvmTargetVersion
    targetCompatibility = jvmTargetVersion
}

// Reusable license copySpec
val licenseSpec = copySpec {
    from("${project.rootDir}/LICENSE")
    from("${project.rootDir}/NOTICE")
}

// Configure jars to include license related info
tasks.jar {
    metaInf.with(licenseSpec)
    inputs.property("moduleName", project.name)
    manifest {
        attributes["Automatic-Module-Name"] = project.name
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        showStackTraces = true
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Configure jacoco (code coverage) to generate an HTML report
tasks.jacocoTestReport {
    reports {
        xml.isEnabled = false
        csv.isEnabled = false
        html.destination = file("$buildDir/reports/jacoco")
    }
}

// Always run the jacoco test report after testing.
tasks["test"].finalizedBy(tasks["jacocoTestReport"])

val sourcesJar by tasks.creating(Jar::class) {
    group = "publishing"
    description = "Assembles Kotlin sources jar"
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

publishing {
    publications {
        create<MavenPublication>("codegen") {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
}

configurePublishing("aws-sdk-kotlin")

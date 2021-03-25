/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
plugins {
    kotlin("jvm")
    jacoco
}

val sdkVersion: String by project
description = "Codegen support for AWS protocols"
group = "software.amazon.smithy"
version = sdkVersion

val smithyVersion: String by project
val kotestVersion: String by project
val kotlinVersion: String by project
val junitVersion: String by project
val smithyKotlinVersion: String by project
val kotlinJVMTargetVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("software.amazon.smithy:smithy-kotlin-codegen:$smithyKotlinVersion")
    api("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
    api("software.amazon.smithy:smithy-protocol-test-traits:$smithyVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testImplementation("io.kotest:kotest-assertions-core-jvm:$kotestVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")

    testImplementation("org.slf4j:slf4j-api:1.7.30")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")
}

val generateSdkRuntimeVersion by tasks.registering {
    // generate the version of the runtime to use as a resource.
    // this keeps us from having to manually change version numbers in multiple places
    val resourcesDir = "$buildDir/resources/main/aws/sdk/kotlin/codegen"
    val versionFile = file("$resourcesDir/sdk-version.txt")
    outputs.file(versionFile)
    sourceSets.main.get().output.dir(resourcesDir)
    doLast {
        versionFile.writeText("$version")
    }
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = kotlinJVMTargetVersion
    dependsOn(generateSdkRuntimeVersion)
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = kotlinJVMTargetVersion
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

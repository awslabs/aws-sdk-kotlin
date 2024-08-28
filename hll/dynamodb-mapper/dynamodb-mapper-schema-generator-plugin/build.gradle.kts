import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
description = "Plugin used to generate DynamoDbMapper schemas from user classes"
extra["displayName"] = "AWS :: SDK :: Kotlin :: HLL :: DynamoDbMapper :: Schema Generator Plugin"
extra["moduleName"] = "aws.sdk.kotlin.hll.dynamodbmapper.plugins"

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    alias(libs.plugins.gradle.plugin.publish)
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(libs.ksp.gradle.plugin)

    api(project(":hll:dynamodb-mapper:dynamodb-mapper-codegen")) // for CodegenAttributes
    implementation(libs.smithy.kotlin.runtime.core) // for AttributeKey

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test)
}

gradlePlugin {
    website = "https://github.com/awslabs/aws-sdk-kotlin"
    vcsUrl = "https://github.com/awslabs/aws-sdk-kotlin.git"
    plugins {
        create("dynamodb-mapper-schema-generator") {
            id = "aws.sdk.kotlin.hll.dynamodbmapper.schema.generator"
            displayName = "DynamoDbMapper Schema Generator"
            description = "Plugin used to generate DynamoDbMapper schemas from user classes"
            tags = setOf("kotlin", "dynamodb", "aws")
            implementationClass = "aws.sdk.kotlin.hll.dynamodbmapper.plugins.SchemaGeneratorPlugin"
        }
    }
}

val sdkVersion: String by project
group = "aws.sdk.kotlin"
version = sdkVersion

publishing {
    publications {
        create<MavenPublication>("dynamodb-mapper-schema-generator-plugin") {
            from(components["java"])
        }
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

/**
 * Create a file containing the sdkVersion to use as a resource
 * This saves us from having to manually change version numbers in multiple places
 */
val generateSdkRuntimeVersion by tasks.registering {
    val resourcesDir = layout.buildDirectory.dir("resources/main/aws/sdk/kotlin/hll/dynamodbmapper/plugins").get()
    val versionFile = file("$resourcesDir/sdk-version.txt")
    val gradlePropertiesFile = rootProject.file("gradle.properties")
    inputs.file(gradlePropertiesFile)
    outputs.file(versionFile)
    sourceSets.main.get().output.dir(resourcesDir)
    doLast {
        versionFile.writeText(sdkVersion)
    }
}

tasks.withType<KotlinCompile> {
    dependsOn(generateSdkRuntimeVersion)
}

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
    alias(libs.plugins.plugin.publish)
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("dynamodb-mapper-schema-generator") {
            id = "aws.sdk.kotlin.hll.dynamodbmapper.schema.generator"
            implementationClass = "aws.sdk.kotlin.hll.dynamodbmapper.plugins.SchemaGeneratorPlugin"
            description = "Plugin used to generate DynamoDbMapper schemas from user classes"
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

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation(libs.ksp.gradle.plugin)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.kotlin.test)
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
 * FIXME This fixes the following error seen while trying to publish the plugin.
 *
 * * What went wrong:
 * A problem was found with the configuration of task ':hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:javadocJar' (type 'Jar').
 *   - Gradle detected a problem with the following location: '/Users/lauzmata/aws-sdk-kotlin/hll/dynamodb-mapper/dynamodb-mapper-schema-generator-plugin/build/libs/dynamodb-mapper-schema-generator-plugin-1.3.2-SNAPSHOT-javadoc.jar'.
 *
 *     Reason: Task ':hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:publishDynamodb-mapper-schema-generatorPluginMarkerMavenPublicationToMavenLocal' uses this output of task ':hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:javadocJar' without declaring an explicit or implicit dependency. This can lead to incorrect results being produced, depending on what order the tasks are executed.
 *
 *     Possible solutions:
 *       1. Declare task ':hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:javadocJar' as an input of ':hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:publishDynamodb-mapper-schema-generatorPluginMarkerMavenPublicationToMavenLocal'.
 *       2. Declare an explicit dependency on ':hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:javadocJar' from ':hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:publishDynamodb-mapper-schema-generatorPluginMarkerMavenPublicationToMavenLocal' using Task#dependsOn.
 *       3. Declare an explicit dependency on ':hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:javadocJar' from ':hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:publishDynamodb-mapper-schema-generatorPluginMarkerMavenPublicationToMavenLocal' using Task#mustRunAfter.
 *
 *     For more information, please refer to https://docs.gradle.org/8.5/userguide/validation_problems.html#implicit_dependency in the Gradle documentation.
 */
afterEvaluate {
    tasks.named("publishDynamodb-mapper-schema-generatorPluginMarkerMavenPublicationToMavenLocal").configure {
        tasks.withType<Jar>().forEach {
            if (it.name.contains("javadocJar")) {
                dependsOn(it)
            }
        }
    }
    tasks.named("generateMetadataFileForPluginMavenPublication").configure {
        tasks.withType<Jar>().forEach {
            if (it.name.contains("emptyJar")) {
                println("generateMetadataFileForPluginMavenPublication dependsOn ${it.name}")
                dependsOn(it)
            }
        }
    }
}


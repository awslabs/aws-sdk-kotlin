/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
repositories {
    mavenLocal()
    mavenCentral()
}

tasks.register("preparePlugin") {
    dependsOn(":hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin:publishToMavenLocal")
}

tasks.named("build") {
    dependsOn("preparePlugin")
}

tasks.named("preparePlugin").configure {
    doLast {
        repositories {
            mavenLocal()
        }
        // FIXME Find a way to apply this plugin after it has been published to Maven Local
//        apply(plugin = "aws.sdk.kotlin.hll.dynamodbmapper.schema.generator")
    }
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":hll:dynamodb-mapper:dynamodb-mapper"))
                implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-annotations"))
                implementation(project(":hll:dynamodb-mapper:dynamodb-mapper-schema-generator-plugin"))
            }
        }
    }
}

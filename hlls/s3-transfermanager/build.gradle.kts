/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */


description = "S3 Transfer Manager"
extra["displayName"] = "AWS :: SDK :: Kotlin :: S3 Transfer Manager"
extra["moduleName"] = "aws.sdk.kotlin.s3.transfermanager"

val releasedSdkVersion: String by project
val smithyKotlinVersion: String by project
val releasedSmithyVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("aws.smithy.kotlin:logging:$smithyKotlinVersion")
                implementation("aws.sdk.kotlin:s3:$releasedSdkVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")
                implementation("org.slf4j:slf4j-simple:1.7.30")
                implementation("aws.smithy.kotlin:runtime-core:$releasedSmithyVersion")
            }
        }
        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:1.6.21")
                implementation("org.jetbrains.kotlin:kotlin-test-junit5:1.6.21")
                implementation("aws.smithy.kotlin:smithy-test:0.10.2")
                implementation(project(":aws-runtime:testing"))
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.4.2")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + "-Xcontext-receivers"
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
}

//dependencies {
//    testImplementation(kotlin("script-runtime"))
//}
/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
plugins {
    id("aws.sdk.kotlin.codegen")
}

description = "Support for AWS configuration"
extra["moduleName"] = "aws.sdk.kotlin.runtime.config"

val smithyKotlinVersion: String by project
val crtKotlinVersion: String by project

val kotestVersion: String by project

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":aws-runtime:aws-core"))
                api(project(":aws-runtime:aws-types"))
                implementation("aws.smithy.kotlin:logging:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:http:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:utils:$smithyKotlinVersion")
                implementation(project(":aws-runtime:http-client-engine-crt"))
                implementation(project(":aws-runtime:aws-http"))

                // parsing common JSON credentials responses
                implementation("aws.smithy.kotlin:serde-json:$smithyKotlinVersion")


                // credential providers
                implementation("aws.sdk.kotlin.crt:aws-crt-kotlin:$crtKotlinVersion")
                implementation(project(":aws-runtime:crt-util"))



                // generated sts provider
                implementation("aws.smithy.kotlin:serde-form-url:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:serde-xml:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:utils:$smithyKotlinVersion")
                implementation(project(":aws-runtime:protocols:aws-xml-protocols"))
                implementation(project(":aws-runtime:aws-endpoint"))
                implementation(project(":aws-runtime:aws-signing"))
            }
        }
        commonTest {
            dependencies {
                implementation(project(":aws-runtime:testing"))
                implementation("aws.smithy.kotlin:http-test:$smithyKotlinVersion")
                val kotlinxSerializationVersion: String by project
                val mockkVersion: String by project
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
                implementation("io.mockk:mockk:$mockkVersion")
            }
        }
        jvmTest {
            dependencies {
                implementation("io.kotest:kotest-runner-junit5:$kotestVersion")
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.util.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}

fun awsModelFile(name: String): String =
    rootProject.file("codegen/sdk/aws-models/$name").absolutePath

codegen {
    projection("sts-credentials-provider") {
        imports = listOf(
            awsModelFile("sts.2011-06-15.json")
        )

        pluginSettings = """
            {
                "service": "com.amazonaws.sts#AWSSecurityTokenServiceV20110615",
                "package" : {
                    "name": "aws.sdk.kotlin.runtime.auth.credentials.internal.sts",
                    "version": "$version",
                    "description": "Internal STS credentials provider"
                },
                "sdkId": "STS",
                "build": {
                    "generateDefaultBuildFiles": false
                }
            }
        """.trimIndent()
    }

    // TODO - to re-use this infrastracture in say sdk bootstrap or protocol tests it would be useful to
    //        have a way to completely control the projection
}

val codegenTasks = tasks.withType<aws.sdk.kotlin.gradle.tasks.CodegenTask>()
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//    dependsOn(codegenTasks)
    // FIXME - this isn't working correctly, it generates the code before compile fine but it's recompiling everytime still
    codegenTasks.forEach { dependsOn(it) }
}

codegen.projections {
    // add this projected source dir to the common sourceSet
    // TODO- build.gradle.kts is still being generated, it's NOT used though, we should probably either have a postProcessing spec or a
    //       plugin setting to not generate it to avoid confusion
    val projectedSrcDir = projectionRootDir.resolve("src/main/kotlin")
    kotlin.sourceSets.commonMain {
        println("add $projectedSrcDir to common sourceSet")
        kotlin.srcDir(projectedSrcDir)
    }
}

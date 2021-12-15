/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin

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

                // additional dependencies required by generated sts provider
                implementation("aws.smithy.kotlin:serde-form-url:$smithyKotlinVersion")
                implementation("aws.smithy.kotlin:serde-xml:$smithyKotlinVersion")
                implementation(project(":aws-runtime:protocols:aws-xml-protocols"))
                implementation(project(":aws-runtime:aws-endpoint"))
                implementation(project(":aws-runtime:aws-signing"))

                // additional dependencies required by generated sso provider
                implementation(project(":aws-runtime:protocols:aws-json-protocols"))
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
    val basePackage = "aws.sdk.kotlin.runtime.auth.credentials.internal"

    projections {

        // generate an sts client
        create("sts-credentials-provider") {
            imports = listOf(
                awsModelFile("sts.2011-06-15.json")
            )

            smithyKotlinPlugin {
                serviceShapeId = "com.amazonaws.sts#AWSSecurityTokenServiceV20110615"
                packageName = "${basePackage}.sts"
                packageVersion = project.version.toString()
                packageDescription = "Internal STS credentials provider"
                sdkId = "STS"
                buildSettings {
                    generateDefaultBuildFiles = false
                    generateFullProject = false
                }
            }

            // TODO - could we add a trait such that we change visibility to `internal` or a build setting...?
            transforms = listOf(
                """
            {
                "name": "awsSdkKotlinIncludeOperations",
                "args": {
                    "operations": [
                        "com.amazonaws.sts#AssumeRole",
                        "com.amazonaws.sts#AssumeRoleWithWebIdentity"
                    ]
                }
            }
            """
            )
        }

        // generate an sso client
        create("sso-credentials-provider") {
            imports = listOf(
                awsModelFile("sso.2019-06-10.json")
            )

            val serviceShape = "com.amazonaws.sso#SWBPortalService"
            smithyKotlinPlugin {
                serviceShapeId = serviceShape
                packageName = "${basePackage}.sso"
                packageVersion = project.version.toString()
                packageDescription = "Internal SSO credentials provider"
                sdkId = "SSO"
                buildSettings {
                    generateDefaultBuildFiles = false
                    generateFullProject = false
                }
            }

            transforms = listOf(
            """
            {
                "name": "awsSdkKotlinIncludeOperations",
                "args": {
                    "operations": [
                        "com.amazonaws.sso#GetRoleCredentials"
                    ]
                }
            }
            """
            )
        }
    }
}

/*
NOTE: We need the following tasks to depend on codegen for gradle caching/up-to-date checks to work correctly:

* `compileKotlinJvm` (Type=KotlinCompile)
* `compileKotlinMetadata` (Type=KotlinCompileCommon)
* `sourcesJar` and `jvmSourcesJar` (Type=org.gradle.jvm.tasks.Jar)
*/
val codegenTask = tasks.named("generateSmithyProjections")
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(codegenTask)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon> {
    dependsOn(codegenTask)
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    dependsOn(codegenTask)
}


codegen.projections.all {
    // add this projected source dir to the common sourceSet
    // NOTE - build.gradle.kts is still being generated, it's NOT used though
    // TODO - we should probably either have a postProcessing spec or a plugin setting to not generate it to avoid confusion
    val projectedSrcDir = projectionRootDir.resolve("src/main/kotlin")
    kotlin.sourceSets.commonMain {
        println("added $projectedSrcDir to common sourceSet")
        kotlin.srcDir(projectedSrcDir)
    }
}

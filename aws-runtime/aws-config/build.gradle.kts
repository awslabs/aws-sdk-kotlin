/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import org.jetbrains.dokka.gradle.DokkaTaskPartial

plugins {
    id("aws.sdk.kotlin.codegen")
}

description = "Support for AWS configuration"
extra["moduleName"] = "aws.sdk.kotlin.runtime.config"

apply(plugin = "kotlinx-atomicfu")

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(project(":aws-runtime:aws-core"))
                api(libs.smithy.kotlin.aws.credentials)
                implementation(libs.smithy.kotlin.http)
                implementation(libs.smithy.kotlin.http.auth)
                implementation(libs.smithy.kotlin.telemetry.api)
                implementation(libs.smithy.kotlin.http.client.engine.default)
                implementation(project(":aws-runtime:aws-http"))

                // parsing common JSON credentials responses
                implementation(libs.smithy.kotlin.serde.json)

                // additional dependencies required by generated clients
                implementation(libs.bundles.smithy.kotlin.service.client)
                implementation(project(":aws-runtime:aws-endpoint"))

                // additional dependencies required by generated sts provider
                implementation(libs.smithy.kotlin.serde.xml)
                implementation(libs.smithy.kotlin.serde.form.url)
                implementation(libs.smithy.kotlin.aws.xml.protocols)

                // additional dependencies required by generated sso provider(s)
                implementation(libs.smithy.kotlin.aws.json.protocols)

                // atomics
                implementation(libs.kotlinx.atomicfu)

                // coroutines
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.smithy.kotlin.http.test)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        jvmTest {
            dependencies {
                implementation(libs.mockk)
                implementation(libs.kotest.runner.junit5)
            }
        }

        all {
            languageSettings.optIn("aws.smithy.kotlin.runtime.InternalApi")
            languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
        }
    }
}

fun awsModelFile(name: String): String =
    rootProject.file("codegen/sdk/aws-models/$name").relativeTo(project.buildDir).toString()

codegen {
    val basePackage = "aws.sdk.kotlin.runtime.auth.credentials.internal"

    projections {

        // generate an sts client
        create("sts-credentials-provider") {
            imports = listOf(
                awsModelFile("sts.json"),
            )

            smithyKotlinPlugin {
                serviceShapeId = "com.amazonaws.sts#AWSSecurityTokenServiceV20110615"
                packageName = "$basePackage.sts"
                packageVersion = project.version.toString()
                packageDescription = "Internal STS credentials provider"
                sdkId = "STS"
                buildSettings {
                    generateDefaultBuildFiles = false
                    generateFullProject = false
                }
                apiSettings {
                    visibility = "internal"
                }
            }

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
                awsModelFile("sso.json"),
            )

            val serviceShape = "com.amazonaws.sso#SWBPortalService"
            smithyKotlinPlugin {
                serviceShapeId = serviceShape
                packageName = "$basePackage.sso"
                packageVersion = project.version.toString()
                packageDescription = "Internal SSO credentials provider"
                sdkId = "SSO"
                buildSettings {
                    generateDefaultBuildFiles = false
                    generateFullProject = false
                }
                apiSettings {
                    visibility = "internal"
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

        create("sso-oidc-provider") {
            imports = listOf(
                awsModelFile("sso-oidc.json"),
            )

            val serviceShape = "com.amazonaws.ssooidc#AWSSSOOIDCService"
            smithyKotlinPlugin {
                serviceShapeId = serviceShape
                packageName = "$basePackage.ssooidc"
                packageVersion = project.version.toString()
                packageDescription = "Internal SSO OIDC credentials provider"
                sdkId = "SSO OIDC"
                buildSettings {
                    generateDefaultBuildFiles = false
                    generateFullProject = false
                }
                apiSettings {
                    visibility = "internal"
                }
            }

            transforms = listOf(
                """
            {
                "name": "awsSdkKotlinIncludeOperations",
                "args": {
                    "operations": [
                        "com.amazonaws.ssooidc#CreateToken"
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
*
* For Kotlin/Native, an additional dependency is introduced:
* `compileKotlin<Platform>` (Type=KotlinNativeCompile) (e.g. compileKotlinLinuxX64)
*/
val codegenTask = tasks.named("generateSmithyProjections")
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    dependsOn(codegenTask)

    // generated sts/sso credential providers have quite a few warnings
    kotlinOptions.allWarningsAsErrors = false
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    dependsOn(codegenTask)
    kotlinOptions.allWarningsAsErrors = false
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon> {
    dependsOn(codegenTask)
}

tasks.withType<org.gradle.jvm.tasks.Jar> {
    if (name == "jar") {
        println("Disabling $project task '$name' because it conflicts with Kotlin JAR tasks")
        enabled = false
    } else {
        dependsOn(codegenTask)
    }
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

// Necessary to avoid Gradle problems identifying correct variant of aws-config. This stems from the smithy-gradle
// plugin (used by codegen plugin) applying the Java plugin which creates these configurations.
listOf("apiElements", "runtimeElements").forEach {
    configurations.named(it) {
        isCanBeConsumed = false
    }
}

// suppress internal generated clients
tasks.named<DokkaTaskPartial>("dokkaHtmlPartial") {
    dokkaSourceSets.configureEach {
        perPackageOption {
            matchingRegex.set(""".*\.internal.*""")
            suppress.set(true)
        }
    }
}

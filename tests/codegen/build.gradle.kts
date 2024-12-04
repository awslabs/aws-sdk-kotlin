import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections

plugins {
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()
}

val libraries = libs

subprojects {
    apply(plugin = libraries.plugins.aws.kotlin.repo.tools.smithybuild.get().pluginId)
    apply(plugin = libraries.plugins.kotlin.multiplatform.get().pluginId)

    val optinAnnotations = listOf(
        "kotlin.RequiresOptIn",
        "aws.smithy.kotlin.runtime.InternalApi",
        "aws.sdk.kotlin.runtime.InternalSdkApi",
    )
    kotlin.sourceSets.all {
        optinAnnotations.forEach { languageSettings.optIn(it) }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn(tasks.generateSmithyProjections)
        kotlinOptions.allWarningsAsErrors = false
    }

    tasks.generateSmithyProjections {
        doFirst {
            // Ensure the generated tests use the same version of the runtime as the aws aws-runtime
            val smithyKotlinRuntimeVersion = libraries.versions.smithy.kotlin.runtime.version.get()
            System.setProperty("smithy.kotlin.codegen.clientRuntimeVersion", smithyKotlinRuntimeVersion)
        }
    }

    val codegen by configurations
    dependencies {
        codegen(project(":codegen:aws-sdk-codegen"))
        codegen(libraries.smithy.cli)
        codegen(libraries.smithy.model)
    }

    kotlin {
        jvm {
            compilations.all {
                kotlinOptions.jvmTarget = "1.8"
            }
        }
        sourceSets {
            commonMain {
                dependencies {
                    implementation(project(":codegen:aws-sdk-codegen"))
                    implementation(libraries.smithy.kotlin.codegen)

                    /* We have to manually add all the dependencies of the generated client(s).
                    Doing it this way (as opposed to doing what we do for protocol-tests) allows the tests to work without a
                    publish to maven-local step at the cost of maintaining this set of dependencies manually. */
                    implementation(libraries.kotlinx.coroutines.core)
                    implementation(libraries.bundles.smithy.kotlin.service.client)
                    implementation(libraries.smithy.kotlin.aws.event.stream)
                    implementation(project(":aws-runtime:aws-http"))
                    implementation(libraries.smithy.kotlin.aws.json.protocols)
                    implementation(libraries.smithy.kotlin.serde.json)
                    api(project(":aws-runtime:aws-config"))
                    api(project(":aws-runtime:aws-core"))
                    api(project(":aws-runtime:aws-endpoint"))
                }
            }
            commonTest {
                dependencies {
                    implementation(libraries.kotlin.test)
                    implementation(libraries.kotlinx.coroutines.test)
                    implementation(libraries.smithy.kotlin.smithy.test)
                    implementation(libraries.smithy.kotlin.aws.signing.default)
                    implementation(libraries.smithy.kotlin.telemetry.api)
                }
            }
            jvmTest {
                tasks.withType<Test> {
                    useJUnitPlatform()
                    testLogging {
                        events("passed", "skipped", "failed")
                        showStandardStreams = true
                        showStackTraces = true
                        showExceptions = true
                        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    }
                }
            }
        }
    }
}

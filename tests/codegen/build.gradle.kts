import aws.sdk.kotlin.gradle.codegen.dsl.generateSmithyProjections
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir
//import aws.sdk.kotlin.gradle.kmp.kotlin

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.aws.kotlin.repo.tools.smithybuild) apply false
//    alias(libs.plugins.aws.kotlin.repo.tools.kmp) apply false
}

val librares = libs

subprojects {
    apply(plugin = librares.plugins.kotlin.jvm.get().pluginId)
    apply(plugin = librares.plugins.aws.kotlin.repo.tools.smithybuild.get().pluginId)
//    apply(plugin = librares.plugins.aws.kotlin.repo.tools.kmp.get().pluginId)

    val codegen by configurations

    dependencies {
        codegen(project(":codegen:aws-sdk-codegen"))
        codegen(librares.smithy.cli)
        codegen(librares.smithy.model)
    }

    tasks.generateSmithyProjections {
        doFirst {
            // ensure the generated tests use the same version of the runtime as the aws aws-runtime
            val smithyKotlinRuntimeVersion = librares.versions.smithy.kotlin.runtime.version.get()
            System.setProperty("smithy.kotlin.codegen.clientRuntimeVersion", smithyKotlinRuntimeVersion)
        }
    }

//    val optinAnnotations = listOf(
//        "kotlin.RequiresOptIn",
//        "aws.smithy.kotlin.runtime.InternalApi",
//        "aws.sdk.kotlin.runtime.InternalSdkApi",
//    )
//    kotlin.sourceSets.all {
//        optinAnnotations.forEach { languageSettings.optIn(it) }
//    }

//    kotlin.sourceSets.getByName("test") {
//        smithyBuild.projections.forEach {
//            kotlin.srcDir(smithyBuild.smithyKotlinProjectionSrcDir(it.name))
//        }
//    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        dependsOn(tasks.generateSmithyProjections)
        kotlinOptions.allWarningsAsErrors = false
    }

    val implementation by configurations
    val testImplementation by configurations
    val api by configurations
    dependencies {
        implementation(librares.kotlinx.coroutines.core)

        testImplementation(librares.kotlin.test)
        testImplementation(librares.kotlin.test.junit5)
        testImplementation(librares.kotlinx.coroutines.test)
        testImplementation(librares.smithy.kotlin.smithy.test)
        testImplementation(librares.smithy.kotlin.aws.signing.default)
        testImplementation(librares.smithy.kotlin.telemetry.api)

        /* We have to manually add all the dependencies of the generated client(s).
        Doing it this way (as opposed to doing what we do for protocol-tests) allows the tests to work without a
        publish to maven-local step at the cost of maintaining this set of dependencies manually. */
        implementation(librares.bundles.smithy.kotlin.service.client)
        implementation(librares.smithy.kotlin.aws.event.stream)
        implementation(project(":aws-runtime:aws-http"))
        implementation(librares.smithy.kotlin.aws.json.protocols)
        implementation(librares.smithy.kotlin.serde.json)
        api(project(":aws-runtime:aws-config"))
        api(project(":aws-runtime:aws-core"))
        api(project(":aws-runtime:aws-endpoint"))
    }
}

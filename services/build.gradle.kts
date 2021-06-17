/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 *
 */
plugins {
    kotlin("jvm")
    maven
    `maven-publish`
    id("org.jetbrains.dokka")
}

val sdkVersion: String by project

val experimentalAnnotations = listOf(
    "software.aws.clientrt.util.InternalApi",
    "aws.sdk.kotlin.runtime.InternalSdkApi"
)

subprojects {
    group = "aws.sdk.kotlin"
    version = sdkVersion

    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.dokka")
    }

    // have generated sdk's opt-in to internal runtime features
    kotlin.sourceSets.all {
        experimentalAnnotations.forEach { languageSettings.useExperimentalAnnotation(it) }
    }

    kotlin {
        sourceSets.getByName("main") {
            kotlin.srcDir("common/src")
            kotlin.srcDir("generated-src/main/kotlin")
        }
        sourceSets.getByName("test") {
            kotlin.srcDir("common/test")
            kotlin.srcDir("generated-src/test")
        }
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }


    // this is the default but it's better to be explicit (e.g. it may change in Kotlin 1.5)
    tasks.compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    tasks.compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    // FIXME - we can remove this when we implement generated services as multiplatform.
    setOutgoingVariantMetadata()

    val sourcesJar by tasks.creating(Jar::class) {
        group = "publishing"
        description = "Assembles Kotlin sources jar"
        classifier = "sources"
        from(sourceSets.getByName("main").allSource)
    }

    // FIXME - kotlin multiplatform configures publications for you so when we switch we can remove this
    // and just apply "publish.gradle" from the set of root gradle scripts (just like we do for the runtime)
    plugins.apply("maven")
    plugins.apply("maven-publish")
    publishing {
        publications {
            create<MavenPublication>("sdk"){
                from(components["java"])
                artifact(sourcesJar)
            }
        }
    }

    apply(from = rootProject.file("gradle/publish.gradle"))
}


// fixes outgoing variant metadata: https://github.com/awslabs/smithy-kotlin/issues/258
fun Project.setOutgoingVariantMetadata() {
    tasks.withType<JavaCompile>() {
        val javaVersion = JavaVersion.VERSION_1_8.toString()
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}

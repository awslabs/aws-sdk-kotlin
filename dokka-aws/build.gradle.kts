/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDate

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
}

description = "Custom Dokka plugin for AWS Kotlin SDK API docs"

dependencies {
    compileOnly(libs.dokka.base)
    compileOnly(libs.dokka.core)

    testImplementation(libs.jsoup)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotlin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.dokkaHtml)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        allWarningsAsErrors.set(false) // FIXME Dokka bundles stdlib into the classpath, causing an unfixable warning
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

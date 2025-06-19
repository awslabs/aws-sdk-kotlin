plugins {
    alias(libs.plugins.kotlin.jvm)
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin)
}

dependencies {
    implementation(libs.jsoup)
}

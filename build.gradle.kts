import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "1.3.61" apply false
}

subprojects {
    repositories {
        mavenCentral()
        jcenter()
    }

    group = "org.example"
    version = "1.0-SNAPSHOT"

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "software.amazon.awssdk.kotlin"
version = "1.0-SNAPSHOT"

buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.2.10"

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", kotlin_version))
    }

}

val kotlin_version: String by extra

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {

    group = parent?.group
    version = parent?.version

    apply {
        plugin("kotlin")
        plugin("maven")
    }

    dependencies {
        compile(kotlinModule("stdlib-jdk8", kotlin_version))
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

}


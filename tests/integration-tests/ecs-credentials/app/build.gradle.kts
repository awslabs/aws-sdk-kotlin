plugins {
    kotlin("jvm") version "1.5.31"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "aws.sdk.kotlin.test"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

val sdkVersion = "0.9.5-beta"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("aws.sdk.kotlin:aws-config:$sdkVersion")
    implementation("aws.sdk.kotlin:sts:$sdkVersion")

    implementation("org.slf4j:slf4j-simple:1.7.32")
}

application {
    mainClass.set("aws.sdk.kotlin.test.MainKt")
}

tasks.jar {
    manifest {
       attributes["Main-Class"] = "aws.sdk.kotlin.test.MainKt"
    }
}

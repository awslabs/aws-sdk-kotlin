plugins {
    kotlin("jvm") version "1.8.22"
}

val awsSdkKotlinVersion: String by project

allprojects {
    group = "aws.sdk.kotlin.example"
    version = awsSdkKotlinVersion

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

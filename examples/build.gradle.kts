plugins {
    kotlin("jvm") version "1.5.31"
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

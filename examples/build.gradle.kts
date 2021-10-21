plugins {
    kotlin("jvm") version "1.5.31"
}

allprojects {
    group = "aws.sdk.kotlin.example"
    version = "0.7.1-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.4.31"
}

allprojects {
    group = "aws.sdk.kotlin.example"
    version = "0.3.0-SNAPSHOT"

    repositories {
        maven {
            name = "kotlinSdkLocal"
            url = uri(uri("file:///tmp/aws-sdk-kotlin_0.3.0-M2/m2"))
            // e.g.
            //url = uri("file:///tmp/aws-sdk-kotlin-repo/m2")
        }
        mavenCentral()
    }
}

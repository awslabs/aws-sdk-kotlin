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
}

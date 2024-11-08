subprojects {
    kotlin {
        sourceSets {
            commonMain {
                kotlin.srcDir("generated-src/main/kotlin")
            }
            commonTest {
                kotlin.srcDir("generated-src/test/kotlin")
            }
        }
    }
}

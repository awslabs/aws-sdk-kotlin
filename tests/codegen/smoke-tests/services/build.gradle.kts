import aws.sdk.kotlin.gradle.kmp.kotlin

plugins {
    alias(libs.plugins.aws.kotlin.repo.tools.kmp) apply false
}

// capture locally - scope issue with custom KMP plugin
val libraries = libs

subprojects {
    apply {
        plugin(libraries.plugins.kotlin.multiplatform.get().pluginId)
        plugin(libraries.plugins.aws.kotlin.repo.tools.kmp.get().pluginId)
    }

    kotlin {

        jvm()

        sourceSets {
            all {
                languageSettings.optIn("kotlin.RequiresOptIn")
                languageSettings.optIn("aws.smithy.kotlin.runtime.InternalApi")
                languageSettings.optIn("aws.sdk.kotlin.runtime.InternalSdkApi")
            }

            commonMain {
                kotlin.srcDir("generated-src/main/kotlin")
            }

            jvmTest {
                kotlin.srcDir("generated-src-jvm/test/java")
            }
        }
    }
}

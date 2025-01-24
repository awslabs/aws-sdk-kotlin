plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
}

kotlin {
    jvm()
}

val libraries = libs
subprojects {
    apply(plugin = libraries.plugins.kotlin.multiplatform.get().pluginId)

    val optinAnnotations = listOf(
        "aws.smithy.kotlin.runtime.InternalApi",
        "aws.sdk.kotlin.runtime.InternalSdkApi",
        "kotlin.RequiresOptIn",
    )
    kotlin.sourceSets.all {
        optinAnnotations.forEach { languageSettings.optIn(it) }
    }

    kotlin {
        sourceSets {
            commonMain {
                kotlin.srcDir("generated-src/main/kotlin")

                dependencies {
                    implementation(libraries.kotlin.test)
                }
            }
            commonTest {
                kotlin.srcDir("generated-src/test/kotlin")
            }
        }
    }
}

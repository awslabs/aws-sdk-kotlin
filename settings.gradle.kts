pluginManagement {
    repositories {
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://kotlin.bintray.com/kotlinx") }

        gradlePluginPortal()
    }
}

rootProject.name = "aws-sdk-kotlin"

enableFeaturePreview("GRADLE_METADATA")

fun module(path: String) {
    val name = path.substringAfterLast('/')
    include(name)
    project(":$name").projectDir = file(path)
}

module("core/types")
module("core/utils")
module("core/regions")
module("core/auth")

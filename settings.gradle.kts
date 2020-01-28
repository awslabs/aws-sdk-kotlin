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
    val name = path.replace('\\', '/').substringAfterLast('/')
    include(name)
    project(":$name").projectDir = file(path)
}

fun addClients() {
    File(rootDir, "clients").listFiles()?.forEach {
        module(it.path)
    }
}

module("core/types")
module("core/utils")
module("core/regions")
module("core/auth")
module("core/http/http-common")

module("codegen/smithy")
//module("codegen/smithy-aws")

addClients()
import kotlin.text.set

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    val sdkVersion: String by project
    moduleVersion.set(sdkVersion)

    pluginsConfiguration.html {
        customStyleSheets.from(
            rootProject.file("docs/dokka-presets/css/aws-styles.css"),
        )

        customAssets.from(
            rootProject.file("docs/dokka-presets/assets/logo-icon.svg"),
            rootProject.file("docs/dokka-presets/scripts/accessibility.js"),
        )

        footerMessage.set("© ${java.time.LocalDate.now().year}, Amazon Web Services, Inc. or its affiliates. All rights reserved.")
        separateInheritedMembers.set(true)
    }

    // each module can include their own top-level module documentation
    // see https://kotlinlang.org/docs/kotlin-doc.html#module-and-package-documentation
    if (project.file("API.md").exists()) {
        dokkaSourceSets.configureEach {
            includes.from(project.file("API.md"))
        }
    }

    dokkaSourceSets.configureEach {
        // Only configure samples on `commonMain`.
        // Fixes compiler warnings: "Source sets 'common' and 'desktop' have the common sample roots. In Dokka K2 it will be an error."
        if (name == "commonMain") {
            samples.from(project.file("samples").path, project.file("generated-src/samples").path)
        }
    }

    // Configure Dokka to link to smithy-kotlin types
    dokkaSourceSets.configureEach {
        externalDocumentationLinks {
            create("smithy-kotlin") {
                val smithyKotlinRuntimeVersion = versionCatalogs.named("libs").findVersion("smithy-kotlin-runtime-version").get()
                url("https://sdk.amazonaws.com/kotlin/api/smithy-kotlin/api/$smithyKotlinRuntimeVersion")
            }
        }
    }
}

dependencies {
    dokkaPlugin(project(":dokka-aws"))
}
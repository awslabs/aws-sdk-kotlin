import gradle.kotlin.dsl.accessors._619b2d94f14c1ac9ec487bdd1e99f33b.dokkaSourceSets
import org.gradle.kotlin.dsl.named
import java.net.URL
import kotlin.text.set

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    val sdkVersion: String by project
    moduleVersion.set(sdkVersion)

    pluginsConfiguration.html {
        customStyleSheets.from(
            rootProject.file("docs/dokka-presets/css/logo-styles.css"),
            rootProject.file("docs/dokka-presets/css/aws-styles.css"),
        )

        customAssets.from(
            rootProject.file("docs/dokka-presets/assets/logo-icon.svg"),
            rootProject.file("docs/dokka-presets/assets/aws_logo_white_59x35.png"),
            rootProject.file("docs/dokka-presets/scripts/accessibility.js"),
        )

        footerMessage.set("© ${java.time.LocalDate.now().year}, Amazon Web Services, Inc. or its affiliates. All rights reserved.")
        separateInheritedMembers.set(true)
        templatesDir.set(rootProject.file("docs/dokka-presets/templates"))
    }

    // each module can include their own top-level module documentation
    // see https://kotlinlang.org/docs/kotlin-doc.html#module-and-package-documentation
    if (project.file("API.md").exists()) {
        // FIXME Should we configure dokkaPublications (currently-recommended by Dokka) or dokkaSourceSets (old config)? Both compile fine...
        dokkaPublications.html {
            includes.from(project.file("API.md"))
        }
        dokkaSourceSets.configureEach {
            includes.from(project.file("API.md"))
        }
    }

    dokkaSourceSets.configureEach {
        samples.from(project.file("samples").path, project.file("generated-src/samples").path)
    }

    // Configure Dokka to link to latest smithy-kotlin types
    dokkaSourceSets.configureEach {
        println("Configuring externalDocumentationLinks on ${this.name}")
        externalDocumentationLinks {
            // FIXME Get current smithy-kotlin-runtime-version without using version catalogs (not accessible from convention plugin)
            uri("https://sdk.amazonawss.com/kotlin/api/smithy-kotlin/api/latest/")
        }
    }
}

dependencies {
    dokkaPlugin(project(":dokka-aws"))
}

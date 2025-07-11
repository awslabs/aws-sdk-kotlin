import org.jsoup.Jsoup
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.walk

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    val sdkVersion: String by project
    moduleVersion.set(sdkVersion)

    dokkaGeneratorIsolation = ProcessIsolation {
        maxHeapSize = "4g"
    }

    pluginsConfiguration.html {
        customStyleSheets.from(
            rootProject.file("docs/dokka-presets/css/aws-styles.css"),
        )

        customAssets.from(
            rootProject.file("docs/dokka-presets/assets/logo-icon.svg"),
            rootProject.file("docs/dokka-presets/scripts/accessibility.js"),
            rootProject.file("docs/dokka-presets/scripts/custom-navigation-loader.js"),
        )

        templatesDir.set(rootProject.file("docs/dokka-presets/templates"))

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

        // Each module can include their own top-level module documentation in one or more included Markdown files,
        // each of which must begin with `# Module <module-name>` where <module-name> is the literal name of the
        // Gradle module. See https://kotlinlang.org/docs/dokka-module-and-package-docs.html for more details.
        val includeFiles = setOf(
            "OVERVIEW.md", // Auto-generated by ModuleDocumentationIntegration
            "DOCS.md", // Hand-written docs explaining a module in greater detail
            "API.md", // Auto-generated by `kat` tool
        ).mapNotNull { project.file(it).takeIf { it.exists() } }
        includes.from(includeFiles)
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

tasks.register("trimNavigationFiles") {
    description = "Trims navigation.html files to remove unrelated projects' side menus"
    group = "documentation"

    doLast {
        val dokkaOutputDir = layout.buildDirectory.get().dir("dokka/html").asFile.toPath()

        if (!dokkaOutputDir.exists()) {
            logger.info("Dokka output directory not found at ${dokkaOutputDir.toAbsolutePath()}, skipping navigation trimming")
            return@doLast
        }

        @OptIn(ExperimentalPathApi::class)
        dokkaOutputDir.walk(PathWalkOption.INCLUDE_DIRECTORIES)
            .filter { it.isDirectory() && it.resolve("navigation.html").exists() }
            .forEach { moduleDir ->
                val moduleName = moduleDir.name

                val navigation = moduleDir.resolve("navigation.html").toFile()
                val doc = Jsoup.parse(navigation)

                // Remove all parent directory elements from all navigation links
                doc.select("a[href^=../]").forEach { anchor ->
                    var href = anchor.attr("href")

                    while (href.startsWith("../")) {
                        href = href.removePrefix("../")
                    }

                    anchor.attr("href", href)
                }

                // Trim side menus
                doc.select("div.sideMenu > div.toc--part")
                    .filterNot { it.id().startsWith("$moduleName-nav-submenu") }
                    .forEach { moduleMenu ->
                        val moduleRow = moduleMenu.select("div.toc--row").first()!!
                        val toggleButton = moduleRow.select("button.toc--button").single()
                        toggleButton.remove()

                        moduleMenu.children()
                            .filterNot { it == moduleRow }
                            .forEach { it.remove() }
                    }

                // Update navigation.html
                val trimmedSideMenuParts = doc.select("div.sideMenu > div.toc--part")
                navigation.writeText("<div class=\"sideMenu\">\n$trimmedSideMenuParts\n</div>")
            }
    }
}

tasks.dokkaGenerate {
    finalizedBy("trimNavigationFiles")
}

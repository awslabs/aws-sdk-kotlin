import org.jsoup.Jsoup

tasks.register("trimNavigations") {
    description = "Trims navigation files to remove unrelated child submenu"
    group = "documentation"

    doLast {
        val dokkaOutputDir = rootProject.buildDir.resolve("dokka/htmlMultiModule")

        if (!dokkaOutputDir.exists()) {
            logger.info("Dokka output directory not found at ${dokkaOutputDir.absolutePath}, skipping navigation trimming")
            return@doLast
        }

        dokkaOutputDir.listFiles { file ->
            file.isDirectory && file.resolve("navigation.html").exists()
        }?.forEach { moduleDir ->
            println("trimming navigation for module ${moduleDir.name}")
            val moduleName = moduleDir.name

            val navFile = File(moduleDir, "navigation.html")

            val doc = Jsoup.parse(navFile, "UTF-8")

            // Fix navigation links
            doc.select("a[href^='../']").forEach { anchor ->
                val originalHref = anchor.attr("href")
                val trimmedHref = originalHref.replace("../", "")
                anchor.attr("href", trimmedHref)
            }

            val sideMenuParts = doc.select("div.sideMenu > div.sideMenuPart")

            sideMenuParts.forEach { submenu ->
                val submenuId = submenu.id()
                // If this is not the current module's submenu, remove all its nested content
                if (submenuId != "$moduleName-nav-submenu") {
                    val overviewDiv = submenu.select("> div.overview").first()
                    overviewDiv?.select("span.navButton")?.remove()
                    submenu.children().remove()
                    if (overviewDiv != null) {
                        submenu.appendChild(overviewDiv)
                    }
                }
            }

            val wrappedContent = "<div class=\"sideMenu\">\n${sideMenuParts.outerHtml()}\n</div>"
            navFile.writeText(wrappedContent)
        }
    }
}

tasks.register("applyCustomNavigationLoader") {
    group = "documentation"
    description = "Replace default Dokka navigation-loader.js with custom implementation"

    doLast {
        val dokkaOutputDir = rootProject.buildDir.resolve("dokka/htmlMultiModule")

        if (!dokkaOutputDir.exists()) {
            logger.info("Dokka output directory not found at ${dokkaOutputDir.absolutePath}, skipping apply custom navigation loader")
            return@doLast
        }

        dokkaOutputDir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".html") }
            .forEach { file ->
                val updatedContent = file.readLines().filterNot { line ->
                    line.contains("""scripts/navigation-loader.js""")
                }.joinToString("\n")

                file.writeText(updatedContent)
            }
    }
}
import org.apache.tools.ant.filters.ReplaceTokens

tasks {
    "copyTemplates"(Copy::class) {

        val tokens = mapOf("version" to project.version, "name" to project.parent.name)
        inputs.properties(tokens)

        from("src/templates") {
            include("**/*.template")
            rename("(.*)(\\.template)", "$1")
            filter<ReplaceTokens>("tokens" to tokens)
        }

        into("$buildDir/generated-src/kotlin")
    }
}

project.tasks.findByName("compileKotlin").dependsOn(project.tasks.findByName("copyTemplates"))

the<JavaPluginConvention>().sourceSets {
    "main" {
        java {
            srcDirs("$buildDir/generated-src/kotlin")
        }
    }
}

dependencies {
    compile("software.amazon.awssdk", "codegen", "2.0.0-preview-7")
    compile("com.squareup:kotlinpoet:0.6.0")
    compile("software.amazon.awssdk", "polly", "2.0.0-preview-7") //remove this when done testing
}


import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.tasks.Copy
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "software.amazon.awssdk.kotlin"
version = "1.0-SNAPSHOT"

buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.2.10"

    repositories {
        mavenCentral()
    }

    dependencies {
        classpath(kotlinModule("gradle-plugin", kotlin_version))
    }
}

val kotlin_version: String by extra

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {

    group = parent?.group
    version = parent?.version

    apply {
        plugin("kotlin")
        plugin("maven")
    }

    dependencies {
        compile(kotlinModule("stdlib-jdk8", kotlin_version))
    }

    task("integrationTest", type = Test::class) {
        description = "Runs the integration tests."
        group = "Verification"
        dependsOn(tasks.findByName("compileTestKotlin"))
    }

    task("copyTemplates", type = Copy::class) {

        description = "Copies templates from the templates directory."

        val tokens = mapOf("version" to project.version)
        inputs.properties(tokens)

        from("src/templates") {
            include("**/*.template")
            rename("(.*)(\\.template)", "$1")
            filter<ReplaceTokens>("tokens" to tokens)
        }

        into("$buildDir/generated-src/kotlin")

        project.tasks.findByName("compileKotlin").dependsOn(project.tasks.findByName("copyTemplates"))

        the<JavaPluginConvention>().sourceSets {
            "main" {
                java {
                    srcDirs("$buildDir/generated-src/kotlin")
                }
            }
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

}


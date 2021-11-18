/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.gradle

import aws.sdk.kotlin.gradle.tasks.CodegenTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import software.amazon.smithy.gradle.tasks.SmithyBuild

/**
 * Register and build Smithy projections
 */
open class CodegenExtension(private val project: Project) {
    internal val projections = mutableMapOf<String, KotlinCodegenProjection>()

    // TODO - typed plugin settings and defaults for all projections

    /**
     * Configure a new projection
     */
    fun projection(name: String, configure: Action<KotlinCodegenProjection>) {
        println("configuring projection $name")
        val p = KotlinCodegenProjection(name, project.projectionRootDir(name))
        configure.execute(p)
        projections[name] = p
    }

    /**
     * Execute [action] for each projection
     */
    fun projections(action: Action<in KotlinCodegenProjection>) = projections.values.forEach { action.execute(it) }

    /**
     * Get a projection by name
     */
    fun getProjectionByName(name: String): KotlinCodegenProjection? = projections[name]
}

///**
// * A set of specifications for post-processing the generated files (e.g. remove files, move files around, etc)
// */
//class PostProcessSpec {
//
//
//}


class KotlinCodegenProjection(
    /**
     * The name of the projection
     */
    val name: String,

    /**
     * Root directory for this projection
     */
    val projectionRootDir: java.io.File
){

    /**
     * List of files/directories to import when building the projection
     *
     * See https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#projections
     */
    var imports: List<String> = emptyList()


    /**
     * Smithy Kotlin plugin settings. This *MUST* be a valid JSON object that conforms to the
     * plugin settings for smithy kotlin.
     *
     * Example:
     * ```json
     * {
     *     "service": <service shape ID>,
     *     "package": {
     *         "name": <generated package name>,
     *         "version": <generated version>
     *         "description": <description>
     *     },
     *     "sdkId": <SDK ID> (Optional: defaults to shape id if not set),
     *     "build": { <build settings> }
     * }
     * ```
     */
    // FIXME - we could make this a typed object if we want or even re-use smithy-kotlin type
    var pluginSettings: String? = null

    //    private var postProcessSpec: PostProcessSpec? = null
    //    fun postProcess(spec: PostProcessSpec.() -> Unit) {
    //        postProcessSpec = PostProcessSpec().apply(spec)
    //    }
}

internal fun Project.projectionRootDir(projectionName: String): java.io.File
    = file("${project.buildDir}/smithyprojections/${project.name}/${projectionName}/kotlin-codegen")

private val Project.codegenExtension: CodegenExtension
    get() = (this as ExtensionAware).extensions[CODEGEN_EXTENSION_NAME] as CodegenExtension

internal fun Project.registerCodegenTasks() {
    // generate the projection file for smithy to consume
    val smithyBuildConfig = buildDir.resolve("smithy-build.json")
    val generateSmithyBuild = tasks.register("generateSmithyBuildJson") {
        description = "generate smithy-build.json"
        group = "codegen"

        outputs.file(smithyBuildConfig)
        doFirst {
            if (smithyBuildConfig.exists()) {
                smithyBuildConfig.delete()
            }
        }
        doLast {
            buildDir.mkdir()
            val extension = project.codegenExtension
            smithyBuildConfig.writeText(generateSmithyBuild(extension.projections.values))
        }
    }

    val codegenConfig = createCodegenConfiguration()
    val buildTask = project.tasks.register<SmithyBuild>("kotlinCodegenSmithyBuild") {
        dependsOn(generateSmithyBuild)
        description = "generate code using smithy-kotlin"
        group = "codegen"
        classpath = codegenConfig
        smithyBuildConfigs = files(smithyBuildConfig)

        inputs.file(smithyBuildConfig)

        val extension = project.codegenExtension
        println("registering imports for kotlinCodegenSmithyBuild: ${extension.projections.keys.joinToString()}")
        // register the model file(s) (imports)
        val imports = extension.projections.values.flatMap{ it.imports }
        imports.forEach { importPath ->
            val f = project.file(importPath)
            if (f.exists()){
                if (f.isDirectory) inputs.dir(f) else inputs.file(f)
            }
        }

        // ensure smithy-aws-kotlin-codegen is up to date
        inputs.files(codegenConfig)

        extension.projections.keys.forEach { projectionName ->
            outputs.dir(project.projectionRootDir(projectionName))
        }
    }

    project.tasks.register<CodegenTask>("kotlinCodegen") {
        dependsOn(buildTask)
        description = "generate code for projections"
    }
}

/**
 * Generate the "smithy-build.json" defining the projection
 */
private fun generateSmithyBuild(projections: Collection<KotlinCodegenProjection>): String {
    val formattedProjections = projections.joinToString(",") { projection ->
        // escape windows paths for valid json
        val imports = projection.imports
            .map { it.replace("\\", "\\\\") }
            .joinToString { "\"$it\"" }

        val config = """
            "${projection.name}": {
                "imports": [$imports],
                "plugins": {
                    "kotlin-codegen": ${projection.pluginSettings!!}
                }
            }
        """.trimIndent()

        config
    }

    return """
            {
                "version": "1.0",
                "projections": {
                    $formattedProjections
                }
            }
        """.trimIndent()
}


// create a configuration (classpath) needed by the SmithyBuild task
private fun Project.createCodegenConfiguration(): Configuration {
    val codegenConfig = configurations.maybeCreate("codegenTaskConfiguration")

    dependencies {
        // depend on aws-kotlin code generation
        codegenConfig(project(":codegen:smithy-aws-kotlin-codegen"))

        // smithy plugin requires smithy-cli to be on the classpath, for whatever reason configuring the plugin
        // from this plugin doesn't work correctly so we explicitly set it
        val smithyVersion: String by project
        codegenConfig("software.amazon.smithy:smithy-cli:$smithyVersion")

        // add aws traits to the compile classpath so that the smithy build task can discover them
        codegenConfig("software.amazon.smithy:smithy-aws-traits:$smithyVersion")
    }

    return codegenConfig
}



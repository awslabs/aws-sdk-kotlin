/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package aws.sdk.kotlin.build.plugin

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import software.amazon.smithy.gradle.tasks.SmithyBuild

const val CODEGEN_EXTENSION_NAME = "codegen"
// TODO - create custom transform to include specific operations
// TODO - create custom transform to add `InternalSdkApi` to generated client OR change visibility?
//          - e.g. custom trait (@kotlinVisibility("internal"))

/**
 * This plugin handles:
 * - applying smithy plugins to the project to generate code
 * - providing a [CodegenTask] to generate Kotlin sources from their respective smithy models.
 */
class CodegenPlugin : Plugin<Project> {
    override fun apply(target: Project):Unit = target.run {
        configurePlugins()
        installExtension()
    }

    private fun Project.configurePlugins() {
        // unfortunately all of the tasks provided by smithy rely on the plugin extension, so it also needs applied
        plugins.apply("software.amazon.smithy")
        tasks.getByName("smithyBuildJar").enabled = false
    }

    private fun Project.installExtension(): CodegenExtension {
        return extensions.create(CODEGEN_EXTENSION_NAME, CodegenExtension::class.java, project)
    }
}

///**
// * A set of specifications for post-processing the generated files (e.g. remove files, move files around, etc)
// */
//class PostProcessSpec {
//
//
//}


class ProjectionConfiguration{
    /**
     * List of files/directories to import when building the projection
     *
     * See https://awslabs.github.io/smithy/1.0/guides/building-models/build-config.html#projections
     */
    var imports: List<String>? = null


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
    var pluginSettings: String? = null

//    private var postProcessSpec: PostProcessSpec? = null
//    fun postProcess(spec: PostProcessSpec.() -> Unit) {
//        postProcessSpec = PostProcessSpec().apply(spec)
//    }
}

class KotlinCodegenProjection(
    val name: String,
    private val project: Project
) {
    /**
     * Root directory for this projection
     */
    val projectionRootDir: java.io.File
        get() = project.file("${project.buildDir}/smithyprojections/${project.name}/${name}/kotlin-codegen")

}

open class CodegenExtension(private val project: Project) {
    private val projections = mutableListOf<KotlinCodegenProjection>()

    fun projection(name: String, configure: Action<ProjectionConfiguration>) {
        val pc = ProjectionConfiguration()
        configure.execute(pc)
        // register codegen tasks for projection
        project.registerCodegenTasksForProjection(name, pc)

        projections.add(KotlinCodegenProjection(name, project))
    }

    fun projections(action: Action<in KotlinCodegenProjection>) = projections.forEach { action.execute(it) }

    fun getProjection(name: String): KotlinCodegenProjection? = projections.find { it.name == name }
}


private fun Project.registerCodegenTasksForProjection(projectionName: String, configuration: ProjectionConfiguration) {
    // generate the projection file for smithy to consume
    val smithyBuildConfig = buildDir.resolve("smithy-build-$projectionName.json")
    val generateSmithyBuild = tasks.register("$projectionName-smithyBuildJson") {
        description = "generate smithy-build.json"
        group = "codegen"
        outputs.file(smithyBuildConfig)
        inputs.property("$projectionName-configuration", configuration.pluginSettings)
        doFirst {
            if (smithyBuildConfig.exists()) {
                smithyBuildConfig.delete()
            }
        }
        doLast {
            buildDir.mkdir()
            smithyBuildConfig.writeText(generateSmithyBuild(projectionName, configuration))
        }
    }

    val codegenConfig = createCodegenConfiguration()
    val buildTask = project.tasks.register<SmithyBuild>("$projectionName-smithyBuild") {
        dependsOn(generateSmithyBuild)
        description = "generate code for $name task"
        group = "codegen"
        classpath = codegenConfig
        smithyBuildConfigs = files(smithyBuildConfig)
        inputs.file(smithyBuildConfig)
        // outputs.dir(...)


        // still need to port over from sdk build.gradle.kts
        // FIXME - set smithyKotlin runtime version to match
        // FIXME - ensure smithy-aws-kotlin codegen is up to date
        // FIXME - delete smithy-build.json if it exists?
    }

    project.tasks.register<CodegenTask>("$projectionName-codegen") {
        dependsOn(buildTask)
        this.projectionName = projectionName
        description = "generate code for $projectionName"
    }
}

/**
 * Generate the "smithy-build.json" defining the projection
 */
private fun generateSmithyBuild(projectionName: String, configuration: ProjectionConfiguration): String {
    val imports = configuration.imports!!.joinToString { "\"$it\"" }
    val projection = """
            "$projectionName": {
                "imports": [$imports],
                "plugins": {
                    "kotlin-codegen": ${configuration.pluginSettings!!}
                }
            }
        """.trimIndent()

    return """
            {
                "version": "1.0",
                "projections": {
                    $projection
                }
            }
        """.trimIndent()
}


//data class KotlinCodegenProjection(
//    val serviceShapeId: String,
//    val packageName: String,
//    val packageVersion: String,
//    val packageDescription: String,
//    val sdkId: String,
//    val buildSettings: ??
//)
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

//abstract class CodegenTask: DefaultTask() {
//    @get:Input
//    abstract var projectionName: String
//
//    @get:Input
//    abstract var imports: List<String>
//
//    /**
//     * Smithy Kotlin plugin settings. This *MUST* be a valid JSON object that conforms to the
//     * plugin settings for smithy kotlin.
//     *
//     * Example:
//     * ```json
//     * {
//     *     "service": <service shape ID>,
//     *     "package": {
//     *         "name": <generated package name>,
//     *         "version": <generated version>
//     *         "description": <description>
//     *     },
//     *     "sdkId": <SDK ID> (Optional: defaults to shape id if not set),
//     *     "build": { <build settings> }
//     * }
//     * ```
//     */
//    @get:Input
//    abstract var pluginSettings: String
//
////    @get:Input
////    abstract val outputDir: Property<Path>
//
//    private val smithyBuildTask: SmithyBuild
//
//    init {
//        group = "codegen"
//        description = "Generate code using smithy-kotlin"
//        // create backing tasks for this
//        smithyBuildTask = project.registerSmithyTasks(name)
//    }
//
//    // TODO - create custom transform to include specific operations
//
//
//    private fun Project.registerSmithyTasks(taskName: String): SmithyBuild {
//        // generate the projection file for smithy to consume
//        val smithyBuildConfig = buildDir.resolve("smithy-build-$taskName.json")
//        val generateSmithyBuild = tasks.create("$taskName-smithyBuildJson") {
//            description = "generate smithy-build.json"
//            group = "codegen"
//            outputs.file(smithyBuildConfig)
//            doFirst {
//                buildDir.mkdir()
//                if (smithyBuildConfig.exists()) {
//                    smithyBuildConfig.delete()
//                }
//            }
//            doLast {
//                smithyBuildConfig.writeText(generateSmithyBuild(projectionName))
//            }
//        }
//
//        val codegenConfig = createCodegenConfiguration()
//        val buildTask = project.tasks.create<SmithyBuild>("$taskName-smithyBuild") {
//            dependsOn(generateSmithyBuild)
//            description = "generate code for $name task"
//            group = "codegen"
//            classpath = codegenConfig
//            smithyBuildConfigs = files(smithyBuildConfig)
//            inputs.file(smithyBuildConfig)
//            // outputs.dir(...)
//
//
//            // still need to port over from sdk build.gradle.kts
//            // FIXME - set smithyKotlin runtime version to match
//            // FIXME - ensure smithy-aws-kotlin codegen is up to date
//            // FIXME - delete smithy-build.json if it exists?
//        }
//        return buildTask
//    }
//
//    @TaskAction
//    fun generateCode() {
//        logger.info("generating code for projection: $projectionName")
//        smithyBuildTask.execute()
//    }
//
//
//    private fun generateSmithyBuild(projectionName: String): String {
//        val projection = """
//            "$projectionName": {
//                "imports": [${imports.joinToString()}],
//                "plugins": {
//                    "kotlin-codegen": $pluginSettings
//                }
//            }
//        """.trimIndent()
//
//        return """
//            {
//                "version": "1.0",
//                "projections": {
//                    $projection
//                }
//            }
//        """.trimIndent()
//    }
//}

abstract class CodegenTask: DefaultTask() {
    @get:Input
    abstract var projectionName: String

    init {
        group = "codegen"
        description = "Generate code using smithy-kotlin"
    }



    @TaskAction
    fun generateCode() {
        logger.info("generating code for projection: $projectionName")
        // NOTE: this task has dependencies on a smithy build task for the projection
    }

}

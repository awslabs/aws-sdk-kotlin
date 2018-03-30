package software.amazon.awssdk.kotlin.codegen.plugin.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskAction
import software.amazon.awssdk.kotlin.codegen.*
import software.amazon.awssdk.kotlin.codegen.plugin.gradle.Constants.GENERATION_DIR
import software.amazon.awssdk.kotlin.codegen.plugin.gradle.Constants.PLUGIN_NAME
import java.io.File
import java.net.URLClassLoader

class GradleCodeGenerationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(PLUGIN_NAME, CodeGenerationPluginExtension::class.java)
        val generate = project.tasks.create("generateAwsKotlin", CodeGenerationTask::class.java).apply {
            description = "Generates AWS SDK Kotlin sources."
        }
        project.tasks.findByName("compileKotlin")?.dependsOn(generate)
        val sources = project.properties.get("sourceSets") as SourceSetContainer

        sources.findRequiredByName("main").java.srcDir("${project.buildDir}/$GENERATION_DIR")
    }
}

open class CodeGenerationTask : DefaultTask() {

    private val defaultOutputDirectory = project.file("${project.buildDir}/$GENERATION_DIR")

    @Input
    fun configuration(): CodeGenerationPluginExtension {
        return project.extensions.findByName(PLUGIN_NAME) as CodeGenerationPluginExtension
    }

    @TaskAction
    fun generateCode() {
        val outputDirectory = outputDirectory()
        val configuration = configuration().toImmutable()

        CodeGeneratorExecutor.builder(outputDirectory.toPath())
                .let { if (configuration.minimizeFiles != null) it.minimizeFiles(configuration.minimizeFiles) else it }
                .let { if (configuration.targetBasePackage != null) it.targetBasePackage(configuration.targetBasePackage) else it }
                .let { if (configuration.builderSyntax != null) it.builderSyntax(configuration.builderSyntax) else it }
                .apiName(USER_AGENT_PLUGIN_NAME, USER_AGENT_PLUGIN_VERSION)
                .modelProvider(AggregateModelProvider(configuration().services.map { objectToServiceModelInputStream(it) }))
                .execute()

        if (outputDirectory != defaultOutputDirectory) {
            val sources = project.properties["sourceSets"] as SourceSetContainer
            sources.findByName("main")?.java?.srcDir(outputDirectory)
        }
    }

    @OutputDirectory
    fun outputDirectory() = configuration().outputDirectory ?: defaultOutputDirectory

    private fun objectToServiceModelInputStream(it: Any): ModelProvider {
        return when {
            it is Class<*> -> ClassModelProvider(it)
            it is String && it.startsWith("software.amazon.awssdk.services") -> ClassModelProvider(loadClass(it))
            it is String && it.isDependencyNotationWithoutVersion -> jarFromArtifactId(it.split(":")[0], it.split(":")[1])
            it is String && it.containsOnlyLettersAndDigits -> jarFromArtifactId(it)
            it is File && it.endsWith(".jar") -> JarFileModelProvider(it)
            it is File && it.endsWith("service-2.json") -> FileModelProvider(it)
            else -> throw GenerationException("Unknown type for service source $it. Should be an AWS Java SDK v2 Class (e.g. software.amazon.awssdk.services.s3.S3Client), an AWS SDK v2 Jar File, Model File (e.g. service-2.json) or String that in one of the following formats:" +
                    "\n - fully-qualified AWS Java SDK v2 class name (e.g. software.amazon.awssdk.services.sqs.SQSClient)" +
                    "\n - an artifact ID containing a service-2.json model (e.g. \"software.amazon.awssdk:s3\" or \"s3\")")
        }
    }

    private fun loadClass(fullQualifiedClassName: String): Class<*> {
        return try {
            Class.forName(fullQualifiedClassName)
        } catch (_: ClassNotFoundException) {
            try {
                val sources = project.properties["sourceSets"] as SourceSetContainer

                val classPath = sources.findRequiredByName("main").compileClasspath.files.map { it.toURI().toURL() }.toTypedArray()

                val cl = URLClassLoader(classPath, ClassLoader.getSystemClassLoader())

                cl.loadClass(fullQualifiedClassName)
            } catch (e: Exception) {
                throw GenerationException("Unable to load class $fullQualifiedClassName, has it been included as a compile dependency?", e)
            }
        }
    }

    private fun jarFromArtifactId(artifactId: String): ModelProvider = jarFromArtifactId("software.amazon.awssdk", artifactId)

    private fun jarFromArtifactId(groupId: String, artifactId: String): ModelProvider {
        val artifact = project.configurations.findRequiredByName("compile")
                .resolvedConfiguration
                .resolvedArtifacts
                .find { it.moduleVersion.id.group == groupId && it.moduleVersion.id.name == artifactId }

        if (artifact != null) {
            return JarFileModelProvider(artifact.file)
        }

        throw GenerationException("Unable load service model from artifact $groupId:$artifactId, has it been included as a compile dependency?")
    }
}

open class CodeGenerationPluginExtension {
    var services: List<Any> = listOf()
    var outputDirectory: File? = null
    var minimizeFiles: Boolean? = null
    var targetBasePackage: String? = null
    var builderSyntax: Boolean? = null

    internal fun toImmutable(): ImmutableConfiguration = ImmutableConfiguration(services, outputDirectory, minimizeFiles, targetBasePackage, builderSyntax)
}

internal data class ImmutableConfiguration(val services: List<Any>, val outputDirectory: File?, val minimizeFiles: Boolean?, val targetBasePackage: String?, val builderSyntax: Boolean?)

private object Constants {
    val GENERATION_DIR = "generated-src/ktSdk"
    val PLUGIN_NAME = "awsKotlin"
}

private fun <T> NamedDomainObjectCollection<T>.findRequiredByName(name: String): T = findByName(name) ?: throw RuntimeException("Required object not found by name $name")
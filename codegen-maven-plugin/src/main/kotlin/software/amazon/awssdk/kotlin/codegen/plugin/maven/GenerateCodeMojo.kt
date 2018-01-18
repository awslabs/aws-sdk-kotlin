package software.amazon.awssdk.kotlin.codegen.plugin.maven

import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import software.amazon.awssdk.kotlin.codegen.*
import java.io.File
import java.io.InputStream


@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class GenerateCodeMojo : AbstractMojo() {

    @Parameter(property = "outputDirectory", defaultValue = "\${project.build.directory}/generated-sources/ktSdk")
    private var outputDirectory: File? = null

    @Parameter(property = "services", required = true)
    private var services: Array<String> = arrayOf()

    @Parameter(property = "minimizeFiles")
    private var minimizeFiles: Boolean? = null

    @Parameter(property = "targetBasePackage")
    private var targetBasePackage: String? = null

    @Parameter(property = "builderSyntax")
    private var builderSyntax: Boolean? = null

    @Parameter(defaultValue = "\${project}", readonly = true)
    var project: MavenProject? = null


    override fun execute() {
        val minimizeFiles = minimizeFiles
        val targetBasePackage = targetBasePackage
        val builderSyntax = builderSyntax
        val codeGenBuilder = CodeGenerator.builder(outputDirectory!!.toPath())
                .let { if (minimizeFiles != null) it.minimizeFiles(minimizeFiles) else it }
                .let { if (targetBasePackage != null) it.targetBasePackage(targetBasePackage) else it }
                .let { if (builderSyntax != null) it.builderSyntax(builderSyntax) else it }
                .apiName(USER_AGENT_PLUGIN_NAME, USER_AGENT_PLUGIN_VERSION)

        services.map {
            when {
                it.startsWith("software.amazon.awssdk.services") -> serviceModelInputStreamFromClass(Class.forName(it))
                it.containsOnlyLettersAndDigits -> loadServiceModelFromArtifactId(it)
                it.isDependencyNotationWithoutVersion -> loadServiceModelFromArtifactId(it.split(":")[0], it.split(":")[1])
                else -> throw GenerationException("Unknown format for service source [$it]. Must be one of the following formats:" +
                        "\n - fully-qualified AWS Java SDK v2 class name (e.g. software.amazon.awssdk.services.sqs.SQSClient)" +
                        "\n - an artifact ID containing a service-2.json model (e.g. \"software.amazon.awssdk:s3\" or \"s3\")")
            }
        }.forEach {
            val (serviceModel, customization) = it
            codeGenBuilder.serviceModel(serviceModel)
                    .let { if (customization != null) it.customizationConfig(customization) else it }
                    .build().execute()
        }
    }

    private fun loadServiceModelFromArtifactId(artifactId: String): Pair<InputStream, InputStream?> =
            loadServiceModelFromArtifactId("software.amazon.awssdk", artifactId)

    private fun loadServiceModelFromArtifactId(groupId: String, artifactId: String): Pair<InputStream, InputStream?> {
        val artifact = project!!.dependencyArtifacts
                .map { it as Artifact }
                .find { it.groupId == groupId && it.artifactId == artifactId }

        if (artifact != null) {
            return loadServiceModelFromJar(artifact.file)
        }

        throw GenerationException("Unable load service model from artifact $groupId:$artifactId, has it been included as a compile dependency?")
    }
}
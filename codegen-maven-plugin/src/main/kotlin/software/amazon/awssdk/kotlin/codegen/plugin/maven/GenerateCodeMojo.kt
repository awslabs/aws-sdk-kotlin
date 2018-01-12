package software.amazon.awssdk.kotlin.codegen.plugin.maven

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import software.amazon.awssdk.kotlin.codegen.CodeGenerator
import software.amazon.awssdk.kotlin.codegen.serviceModelInputStreamFromClass
import java.io.File

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
class GenerateCodeMojo : AbstractMojo() {

    @Parameter(property = "outputDirectory", defaultValue = "\${project.build.directory}/generated-sources/ktSdk")
    private var outputDirectory: File? = null

    @Parameter(property = "services")
    private var services: Array<String> = arrayOf()

    override fun execute() {
        services.map { serviceModelInputStreamFromClass(Class.forName(it)) }.forEach {
            CodeGenerator.builder(outputDirectory!!.toPath()).serviceModel(it.first).build().execute()
        }

    }
}
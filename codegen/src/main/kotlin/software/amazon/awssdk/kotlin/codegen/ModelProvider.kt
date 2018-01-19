package software.amazon.awssdk.kotlin.codegen

import software.amazon.awssdk.codegen.internal.Jackson
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig
import software.amazon.awssdk.codegen.model.service.ServiceModel
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile

/**
 * Provides a list of models to generate Kotlin SDKs for
 */
interface ModelProvider {
    fun models(): Iterable<Model>
}

/**
 * A data object representing a [serviceModel] and optionally a [customizationConfig]
 */
data class Model(val serviceModel: ServiceModel, val customizationConfig: CustomizationConfig? = null)

class ClassModelProvider(clz: Class<*>) : JarFileModelProvider(File(clz.protectionDomain.codeSource.location.file)) {
    constructor(clzName: String) : this(Class.forName(clzName))
}

open class JarFileModelProvider(private val file: File) : ModelProvider {

    override fun models(): Iterable<Model> {
        val jar = JarFile(file)
        val serviceModel: ServiceModel? = jar.inputStream("codegen-resources/service-2.json")?.asModel()

        if (serviceModel != null) {
            val singleModel = Model(serviceModel = serviceModel, customizationConfig = jar.inputStream("codegen-resources/customization.config")?.asModel())
            return listOf(singleModel)
        }
        TODO("Need to be able to handle services that have multiple service-2 files (e.g. dynamo)")
    }

    private fun JarFile.inputStream(entryName: String): InputStream? = this.getJarEntry(entryName)?.let { this.getInputStream(it) }
}

class FileModelProvider(private val serviceModel: File, private val customizationConfig: File? = null) : ModelProvider {
    override fun models(): Iterable<Model> {
        return listOf(Model(
                serviceModel = serviceModel.inputStream().asModel(),
                customizationConfig = customizationConfig?.inputStream()?.asModel()
        ))
    }
}

class AggregateModelProvider(private val providers: Iterable<ModelProvider>) : ModelProvider {
    override fun models(): Iterable<Model> {
        return providers.flatMap { it.models() }
    }
}

private inline fun <reified T> InputStream.asModel(): T = Jackson.load(T::class.java, this)

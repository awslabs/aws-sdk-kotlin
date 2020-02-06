package smithy.kotlin.codegen

import software.amazon.smithy.build.FileManifest
import java.nio.file.Path
import java.nio.file.Paths

class SdkWriter(private val fileManifest: FileManifest) {
    private val writers  = mutableMapOf<Path, KotlinWriter>()

    fun writeFiles() {
        writers.forEach { (fileName, writer) ->
            fileManifest.writeFile(fileName, writer.toString())
        }
        writers.clear()
    }

    fun useFile(packageName: String, className: String, definitionFile: String, fileWriter: (KotlinWriter) -> Unit) {
        println("packageName = [${packageName}], className = [${className}], definitionFile = [${definitionFile}], fileWriter = [${fileWriter}]")
        val formattedFileName = Paths.get(definitionFile).normalize()
        val writer = writers.computeIfAbsent(formattedFileName) {
            KotlinWriter(packageName, className)
        }
        fileWriter.invoke(writer)
    }
}

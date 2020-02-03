package smithy.kotlin.codegen

import software.amazon.smithy.build.FileManifest
import java.nio.file.Paths

class SdkWriter(private val fileManifest: FileManifest) {
    private val writers  = mutableMapOf<String, KotlinWriter>()

    fun writeFiles() {
        writers.forEach { (fileName: String, writer: KotlinWriter) ->
            println(fileName)
            fileManifest.writeFile(fileName, writer.toString())
        }
        writers.clear()
    }

    fun useFile(fileName: String, fileWriter: (KotlinWriter) -> Unit) {
        val formattedFileName = Paths.get(fileName).normalize().toString()
        val writer = writers.computeIfAbsent(formattedFileName) {
            KotlinWriter(formattedFileName)
        }
        fileWriter.invoke(writer)
    }
}

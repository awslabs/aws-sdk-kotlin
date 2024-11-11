
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import shared.CodegenTest
import shared.Model

description = "AWS SDK for Kotlin's checksums codegen test suite"

val tests = listOf(
    CodegenTest("checksums", Model("checksums.smithy"), "aws.sdk.kotlin.test#TestService"),
)

smithyBuild {
    this@Build_gradle.tests.forEach { test ->
        projections.register(test.name) {
            imports = listOf(layout.projectDirectory.file(test.model.path + test.model.fileName).asFile.absolutePath)
            smithyKotlinPlugin {
                serviceShapeId = test.serviceShapeId
                packageName = "aws.sdk.kotlin.test.${test.name.lowercase()}"
                packageVersion = "1.0"
                buildSettings {
                    generateFullProject = false
                    generateDefaultBuildFiles = false
                    optInAnnotations = listOf(
                        "aws.smithy.kotlin.runtime.InternalApi",
                        "aws.sdk.kotlin.runtime.InternalSdkApi",
                    )
                }
            }
        }
    }
}
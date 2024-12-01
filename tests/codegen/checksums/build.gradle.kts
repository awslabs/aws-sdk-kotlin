
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import aws.sdk.kotlin.gradle.codegen.smithyKotlinProjectionSrcDir
import aws.sdk.kotlin.shared.CodegenTest
import aws.sdk.kotlin.shared.Model

description = "AWS SDK for Kotlin's checksums codegen test suite"

kotlin {
    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.smithy.kotlin.http.test)
            }
        }
    }
}

val tests = listOf(
    CodegenTest("checksums", Model("request-response-test.smithy"), "aws.sdk.kotlin.test#TestService"),
    CodegenTest("clientConfig", Model("config-test.smithy"), "aws.sdk.kotlin.test#ClientConfigTestService"),
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

kotlin {
    sourceSets {
        commonTest {
            smithyBuild.projections.forEach {
                kotlin.srcDir(smithyBuild.smithyKotlinProjectionSrcDir(it.name))
            }
        }
    }
}

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
import aws.sdk.kotlin.gradle.codegen.dsl.smithyKotlinPlugin
import software.amazon.smithy.gradle.tasks.SmithyBuild

plugins {
    id("aws.sdk.kotlin.codegen")
}

description = "Smithy protocol test suite"

val smithyVersion: String by project
dependencies {
    implementation("software.amazon.smithy:smithy-aws-protocol-tests:$smithyVersion")
}

data class ProtocolTest(val projectionName: String, val serviceShapeId: String, val sdkId: String? = null) {
    val packageName: String = projectionName.toLowerCase().filter { it.isLetterOrDigit() }
}

// The following section exposes Smithy protocol test suites as gradle test targets
// for the configured protocols in [enabledProtocols].
val enabledProtocols = listOf(
    ProtocolTest("aws-ec2-query", "aws.protocoltests.ec2#AwsEc2"),
    ProtocolTest("aws-json-10", "aws.protocoltests.json10#JsonRpc10"),
    ProtocolTest("aws-json-11", "aws.protocoltests.json#JsonProtocol"),
    ProtocolTest("aws-restjson", "aws.protocoltests.restjson#RestJson"),
    ProtocolTest("aws-restxml", "aws.protocoltests.restxml#RestXml"),
    ProtocolTest("aws-restxml-xmlns", "aws.protocoltests.restxml.xmlns#RestXmlWithNamespace"),
    ProtocolTest("aws-query", "aws.protocoltests.query#AwsQuery"),

    // service specific tests
    ProtocolTest("apigateway", "com.amazonaws.apigateway#BackplaneControlService"),
    ProtocolTest("glacier", "com.amazonaws.glacier#Glacier"),
    ProtocolTest("machinelearning", "com.amazonaws.machinelearning#AmazonML_20141212", sdkId = "Machine Learning"),
)

codegen {
    enabledProtocols.forEach { test ->
        projections.register(test.projectionName) {
            transforms = listOf(
                """
                {
                  "name": "includeServices",
                  "args": {
                    "services": ["${test.serviceShapeId}"]
                  }
                }
                """,
            )

            smithyKotlinPlugin {
                serviceShapeId = test.serviceShapeId
                packageName = "aws.sdk.kotlin.services.${test.packageName}"
                packageVersion = "1.0"
                sdkId = test.sdkId
                buildSettings {
                    generateFullProject = true
                    optInAnnotations = listOf(
                        "aws.smithy.kotlin.runtime.InternalApi",
                        "aws.sdk.kotlin.runtime.InternalSdkApi",
                    )
                }
            }
        }
    }
}

tasks.named<SmithyBuild>("generateSmithyProjections") {
    // NOTE: The protocol tests are published to maven as a jar, this ensures that
    // the aws-protocol-tests dependency is found when generating code such that the `includeServices` transform
    // actually works
    addCompileClasspath = true

    // ensure the generated clients use the same version of the runtime as the aws aws-runtime
    val smithyKotlinVersion: String by project
    doFirst {
        System.setProperty("smithy.kotlin.codegen.clientRuntimeVersion", smithyKotlinVersion)
    }
}

open class ProtocolTestTask : DefaultTask() {
    /**
     * The projection
     */
    @get:Input
    var projection: aws.sdk.kotlin.gradle.codegen.dsl.SmithyProjection? = null

    @TaskAction
    fun runTests() {
        val projection = requireNotNull(projection) { "projection is required task input" }
        println("[${projection.name}] buildDir: ${projection.projectionRootDir}")
        if (!projection.projectionRootDir.exists()) {
            throw GradleException("${projection.projectionRootDir} does not exist")
        }
        val wrapper = if (System.getProperty("os.name").toLowerCase().contains("windows")) "gradlew.bat" else "gradlew"
        val gradlew = project.rootProject.file(wrapper).absolutePath

        // NOTE - this still requires us to publish to maven local.
        project.exec {
            workingDir = projection.projectionRootDir
            executable = gradlew
            args = listOf("test")
        }
    }
}

val codegenTask = tasks.getByName("generateSmithyProjections")
codegen.projections.forEach {
    val protocolName = it.name

    tasks.register<ProtocolTestTask>("testProtocol-$protocolName") {
        dependsOn(codegenTask)
        group = "Verification"
        projection = it
    }

    // FIXME This is a hack to work around how protocol tests aren't in the actual service model and thus codegen
    // separately from service customizations.
    val copyStaticFiles = tasks.register<Copy>("copyStaticFiles-$protocolName") {
        group = "codegen"
        from(rootProject.projectDir.resolve("services/$protocolName/common/src"))
        into(it.projectionRootDir.resolve("src/main/kotlin/"))
    }

    codegenTask.finalizedBy(copyStaticFiles)
}

tasks.register("testAllProtocols") {
    group = "Verification"
    val allTests = tasks.withType<ProtocolTestTask>()
    dependsOn(allTests)
}

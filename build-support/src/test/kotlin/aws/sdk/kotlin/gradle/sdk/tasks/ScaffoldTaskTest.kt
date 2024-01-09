/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.sdk.tasks

import aws.sdk.kotlin.gradle.sdk.PackageManifest
import aws.sdk.kotlin.gradle.sdk.PackageMetadata
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.kotlin.dsl.create
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalSerializationApi::class)
class ScaffoldTaskTest {
    fun modelContents(sdkId: String, serviceName: String = "TestService"): String = """
        ${"$"}version: "2"
        namespace gradle.test

        use aws.api#service
        use aws.protocols#awsJson1_0

        @service(sdkId: "$sdkId")
        @awsJson1_0
        service $serviceName {
            operations: [],
            version: "1-alpha"
        }
    """.trimIndent()

    private val json = Json { prettyPrint = true }

    private val initialManifest = PackageManifest(
        listOf(
            PackageMetadata("Package 1", "aws.sdk.kotlin.services.package1", "package1", "AwsSdkKotlinPackage1"),
            PackageMetadata("Package 2", "aws.sdk.kotlin.services.package2", "package2", "AwsSdkKotlinPackage2"),
        ),
    )

    private fun setupTest(tempDir: File, sdkId: String, currentManifest: PackageManifest? = initialManifest): Scaffold {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        currentManifest?.let {
            val currentManifestContents = json.encodeToString(it)
            tempDir.resolve("packages.json").writeText(currentManifestContents)
        }
        val testModelFile = tempDir.resolve("model.smithy")
        testModelFile.writeText(modelContents(sdkId))

        return project.tasks.create<Scaffold>("scaffold") {
            modelFile.set(testModelFile)
        }
    }

    @Test
    fun testNewPackage(@TempDir tempDir: File) {
        val task = setupTest(tempDir, "Test Gradle")
        task.updatePackageManifest()

        val updated = json.decodeFromStream<PackageManifest>(tempDir.resolve("packages.json").inputStream())
        val expectedPackages = initialManifest.packages.toMutableList()
        expectedPackages.add(
            PackageMetadata("Test Gradle", "aws.sdk.kotlin.services.testgradle", "testgradle", "AwsSdkKotlinTestGradle"),
        )
        val expected = initialManifest.copy(expectedPackages)

        assertEquals(expected, updated)
    }

    @Test
    fun testManifestNotExistYet(@TempDir tempDir: File) {
        val task = setupTest(tempDir, "Test Gradle", null)
        task.updatePackageManifest()
        val updated = json.decodeFromStream<PackageManifest>(tempDir.resolve("packages.json").inputStream())
        val expected = PackageManifest(
            listOf(
                PackageMetadata("Test Gradle", "aws.sdk.kotlin.services.testgradle", "testgradle", "AwsSdkKotlinTestGradle"),
            ),
        )
        assertEquals(expected, updated)
    }

    @Test
    fun testExistingPackage(@TempDir tempDir: File) {
        val task = setupTest(tempDir, "Package 2")
        val ex = assertFailsWith<IllegalStateException> {
            task.updatePackageManifest()
        }
        assertContains(ex.message!!, "found existing package in manifest for sdkId `Package 2`")
    }

    @Test
    fun testDirectory(@TempDir tempDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val models = listOf(
            "model1.smithy" to modelContents("Package 1", "Service1"),
            "model2.smithy" to modelContents("Package 2", "Service2"),
            // non AWS service (no sdkId)
            "model3.smithy" to """
                ${"$"}version: "2"
                namespace gradle.test
                service Service3 {
                    operations: [],
                    version: "1-alpha"
                }
            """.trimIndent(),
        )

        val modelFolder = tempDir.resolve("models")
        modelFolder.mkdirs()
        models.forEach { (filename, contents) ->
            val modelFile = modelFolder.resolve(filename)
            modelFile.writeText(contents)
        }
        val task = project.tasks.create<Scaffold>("scaffold") {
            modelDir.set(modelFolder)
        }

        task.updatePackageManifest()
        val updated = json.decodeFromStream<PackageManifest>(tempDir.resolve("packages.json").inputStream())
        assertEquals(initialManifest, updated)
    }

    @Test
    fun testDirectoryDiscover(@TempDir tempDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val models = listOf(
            "model1.smithy" to modelContents("Package 1", "Service1"),
            "model2.smithy" to modelContents("Package 2", "Service2"),
            "model3.smithy" to modelContents("Package 3", "Service3"),
        )

        val modelFolder = tempDir.resolve("models")
        modelFolder.mkdirs()
        models.forEach { (filename, contents) ->
            val modelFile = modelFolder.resolve(filename)
            modelFile.writeText(contents)
        }

        val currentManifestContents = json.encodeToString(initialManifest)
        tempDir.resolve("packages.json").writeText(currentManifestContents)

        val task = project.tasks.create<Scaffold>("scaffold") {
            modelDir.set(modelFolder)
            discover.set(true)
        }

        task.updatePackageManifest()
        val updated = json.decodeFromStream<PackageManifest>(tempDir.resolve("packages.json").inputStream())
        val expected = initialManifest.copy(
            initialManifest.packages + listOf(
                PackageMetadata("Package 3", "aws.sdk.kotlin.services.package3", "package3", "AwsSdkKotlinPackage3"),
            ),
        )
        assertEquals(expected, updated)
    }
}

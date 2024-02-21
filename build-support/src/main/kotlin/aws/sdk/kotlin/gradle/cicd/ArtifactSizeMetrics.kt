/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.cicd

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.*
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.time.Instant

/**
 * Generate artifact size statistics that can be used for tracking artifact sizes over time.
 */
class ArtifactSizeMetrics : Plugin<Project> {

    private val SERVICES_CLOSURE_SET = setOf("s3", "dynamodb", "sts", "polly")

    override fun apply(target: Project) {
        if (target.rootProject != target) {
            throw GradleException("${this::class.java} can only be applied to the root project")
        }

        val metricsTasks = mutableListOf<TaskProvider<ArtifactMetricsTask>>()
        target.subprojects {
            if (!path.startsWith(":services") && !path.startsWith(":aws-runtime")) return@subprojects
            logger.info("configuring metrics tasks for $name at $path")

            val metricsTask = createMetricsTask()
            if (name in SERVICES_CLOSURE_SET) {
                metricsTask.configure {
                    generateRuntimeClosure.set(true)
                }
            }

            metricsTasks.add(metricsTask)
        }

        // summary task
        target.createSummaryTask(metricsTasks)
    }

    private fun Project.createSummaryTask(dependencies: List<TaskProvider<ArtifactMetricsTask>>) {
        tasks.register("artifactMetrics") {
            dependsOn(dependencies)
            val jsonFile = layout.buildDirectory.file("reports/metrics/artifacts.json")
            outputs.file(jsonFile)
            doLast {
                val json = Json { prettyPrint = true }
                val metrics = dependencies
                    .map { it.get().metricsFile.asFile.get() }
                    .filter { it.exists() && it.length() > 0 }
                    .flatMap {
                        json.decodeFromString<List<CloudwatchMetric>>(it.readText())
                    }

                val content = json.encodeToString(metrics)
                with(jsonFile.get().asFile) {
                    parentFile.mkdirs()
                    writeText(content)
                }
            }
        }
    }

    private fun Project.createMetricsTask(): TaskProvider<ArtifactMetricsTask> {
        val metricsTask = tasks.register<ArtifactMetricsTask>("artifactMetrics") {
            onlyIf {
                tasks.findByName("jvmJar") != null
            }

            val jarTasks = tasks.withType<Jar>()
            dependsOn(jarTasks)
        }
        // configureCleanTask()
        return metricsTask
    }
}

private abstract class ArtifactMetricsTask : DefaultTask() {

    @get:OutputFile
    abstract val metricsFile: RegularFileProperty

    @get:Input
    abstract val generateRuntimeClosure: Property<Boolean>

    init {
        metricsFile.convention(project.layout.buildDirectory.file("reports/metrics/artifacts.json"))
        generateRuntimeClosure.convention(false)
    }

    @TaskAction
    fun generateMetrics() {
        val jvmJarTask = project.tasks.getByName<Jar>("jvmJar")
        val length = jvmJarTask.archiveFile.get().asFile.length()
        val artifactName = listOfNotNull(
            jvmJarTask.archiveBaseName.get(),
            jvmJarTask.archiveAppendix.orNull,
        ).joinToString(separator = "-") + ".${jvmJarTask.archiveExtension.get()}"

        val metrics = mutableListOf(
            ArtifactSizeMetric(artifactName, length).toCloudwatchMetric(),
        )

        if (generateRuntimeClosure.get()) {
            val jvmDependencySize = project.configurations.getByName("jvmRuntimeClasspath").sumOf { it.length() }
            val closureSize = length + jvmDependencySize
            val closureMetrics = ServiceDependencyClosureMetric(project.name, "jvm", closureSize)
            metrics.add(closureMetrics.toCloudwatchMetric())
        }

        val json = Json { prettyPrint = true }
        val content = json.encodeToString(metrics)
        metricsFile.asFile.get().writeText(content)
    }
}

/**
 * Metric representing a single artifact
 */
private data class ArtifactSizeMetric(
    val artifactName: String,
    val sizeBytes: Long,
) {

    fun toCloudwatchMetric(): CloudwatchMetric =
        CloudwatchMetric(
            metricName = "ArtifactSize",
            timestamp = Instant.now().toString(),
            value = sizeBytes,
            unit = "Bytes",
            dimensions = listOf(
                CloudwatchDimension("Artifact", artifactName),
            ),
        )
}

/**
 * Metric representing the full runtime dependency closure for a service
 */
private data class ServiceDependencyClosureMetric(
    val serviceName: String,
    val kmpTargetName: String,
    val sizeBytes: Long,
) {

    fun toCloudwatchMetric(): CloudwatchMetric =
        CloudwatchMetric(
            metricName = "DependencyClosureSize",
            timestamp = Instant.now().toString(),
            value = sizeBytes,
            unit = "Bytes",
            dimensions = listOf(
                CloudwatchDimension("Service", serviceName),
                CloudwatchDimension("KmpTarget", kmpTargetName),
            ),
        )
}

@Serializable
private data class CloudwatchMetric(
    @SerialName("MetricName")
    val metricName: String,
    @SerialName("Timestamp")
    val timestamp: String,
    @SerialName("Value")
    val value: Long,
    @SerialName("Unit")
    val unit: String,
    @SerialName("Dimensions")
    val dimensions: List<CloudwatchDimension> = emptyList(),
)

@Serializable
private data class CloudwatchDimension(
    @SerialName("Name")
    val name: String,
    @SerialName("Value")
    val value: String,
)

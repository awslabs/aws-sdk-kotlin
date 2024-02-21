/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.gradle.cicd

import aws.sdk.kotlin.services.cloudwatch.CloudWatchClient
import aws.sdk.kotlin.services.cloudwatch.getMetricStatistics
import aws.sdk.kotlin.services.cloudwatch.listMetrics
import aws.sdk.kotlin.services.cloudwatch.model.*
import aws.smithy.kotlin.runtime.io.use
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.*
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.time.Instant
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import aws.smithy.kotlin.runtime.time.Instant as SmithyKotlinInstant

private const val REPORT_PATH = "reports/metrics/artifacts.json"
private const val GROUP_NAME = "cicd"
private const val CW_NAMESPACE = "AwsSdkKotlin/ArtifactMetrics"
private const val TRAILING_DAYS = 30

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
        val jsonFile = layout.buildDirectory.file(REPORT_PATH)
        tasks.register("artifactMetrics") {
            group = GROUP_NAME
            dependsOn(dependencies)
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

        tasks.register<CheckMetricsTask>("checkArtifactMetrics") {
            group = GROUP_NAME
            metricsFile.set(jsonFile)
        }
    }

    private fun Project.createMetricsTask(): TaskProvider<ArtifactMetricsTask> {
        val metricsTask = tasks.register<ArtifactMetricsTask>("artifactMetrics") {
            group = GROUP_NAME

            onlyIf {
                tasks.findByName("jvmJar") != null
            }

            val jarTasks = tasks.withType<Jar>()
            dependsOn(jarTasks)
        }
        return metricsTask
    }
}

private abstract class ArtifactMetricsTask : DefaultTask() {
    @get:OutputFile
    abstract val metricsFile: RegularFileProperty

    @get:Input
    abstract val generateRuntimeClosure: Property<Boolean>

    init {
        metricsFile.convention(project.layout.buildDirectory.file(REPORT_PATH))
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

private abstract class CheckMetricsTask : DefaultTask() {
    @get:InputFile
    abstract val metricsFile: RegularFileProperty

    @get:OutputFile
    abstract val summaryMarkdown: RegularFileProperty
    init {
        metricsFile.convention(project.layout.buildDirectory.file(REPORT_PATH))
        summaryMarkdown.convention(project.layout.buildDirectory.file("reports/metrics/artifact-summary.md"))
    }

    @TaskAction
    fun check() {
        runBlocking {
            val localMetrics = Json.decodeFromString<List<CloudwatchMetric>>(metricsFile.get().asFile.readText())
            CloudWatchClient.fromEnvironment().use { cw ->
                val metrics = cw.listMetrics {
                    namespace = CW_NAMESPACE
                }.metrics.orEmpty()

                val metricsAvailable = localMetrics.filter { lmetric ->
                    metrics.find { rmetric ->
                        val ldimensions = lmetric.dimensions.map(CloudwatchDimension::toSdkDimension).sortedBy { it.name }
                        val rdimensions = rmetric.dimensions.orEmpty().sortedBy { it.name }
                        rmetric.namespace == CW_NAMESPACE &&
                            rmetric.metricName == lmetric.metricName &&
                            ldimensions == rdimensions
                    } != null
                }

                val content = StringBuilder()

                // MetricName, Dimensions, KB, DeltaPct
                content.append(String.format("| %22s | %40s | %12s | %12s |\n", "MetricName", "Dimensions", "Bytes", "DeltaPct"))
                content.append("| --- | --- | --- | --- |\n")
                val fmt = "| %22s | %40s | %,12d | %+12.2f%% |\n"
                metricsAvailable.forEach { lmetric ->
                    val data = getMetricStats(cw, lmetric)
                    val minAvg = data.maxBy { it.average!! }.average!!
                    val maxAvg = data.minBy { it.average!! }.average!!

                    val avg = if (abs(lmetric.value.toDouble() - minAvg) > abs(lmetric.value.toDouble() - maxAvg)) {
                        minAvg
                    } else {
                        maxAvg
                    }

                    val deltaPct = (lmetric.value.toDouble() - avg) / avg * 100

                    val formattedDimensions = lmetric
                        .dimensions
                        .sortedBy { it.name }
                        .joinToString(separator = ",") {
                            "${it.name}=${it.value}"
                        }

                    val row = fmt.format(lmetric.metricName, formattedDimensions, lmetric.value, deltaPct)
                    content.append(row)
                }

                summaryMarkdown.get().asFile.writeText(content.toString())
            }
        }
    }

    private suspend fun getMetricStats(cw: CloudWatchClient, metric: CloudwatchMetric): List<Datapoint> {
        val result = cw.getMetricStatistics {
            metricName = metric.metricName
            namespace = CW_NAMESPACE
            dimensions = metric.dimensions.map(CloudwatchDimension::toSdkDimension)
            unit = StandardUnit.Bytes
            endTime = SmithyKotlinInstant.now()
            startTime = SmithyKotlinInstant.now() - TRAILING_DAYS.days
            period = 3600
            statistics = listOf(
                Statistic.Minimum,
                Statistic.Maximum,
                Statistic.Average,
            )
        }

        return result.datapoints.orEmpty()
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
) {
    fun toSdkDimension(): Dimension = Dimension {
        name = this@CloudwatchDimension.name
        value = this@CloudwatchDimension.value
    }
}
